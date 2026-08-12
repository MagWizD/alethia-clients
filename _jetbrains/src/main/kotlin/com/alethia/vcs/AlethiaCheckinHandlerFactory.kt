package com.alethia.vcs

import com.alethia.model.FlaggedRegion
import com.alethia.session.AlethiaStateService
import com.google.gson.GsonBuilder
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory
import git4idea.GitVcs
import java.util.logging.Logger
import git4idea.repo.GitRepositoryManager
import java.io.File

/**
 * Registers Alethia into IntelliJ's commit pipeline.
 * Called by IntelliJ every time a commit is made in any VCS.
 * Creates an AlethiaCheckinHandler per commit operation.
 */
class AlethiaCheckinHandlerFactory : VcsCheckinHandlerFactory(GitVcs.getKey()) {
    override fun createVcsHandler(
        panel: CheckinProjectPanel,
        commitContext: CommitContext
    ): CheckinHandler {
        return AlethiaCheckinHandler(panel)
    }
}

/**
 * Handles a single commit operation.
 * Never blocks or modifies the commit.
 * On successful commit, alethia-state.xml snapshot
 * is created and all queued flags are serialized
 * to git notes and cleared from session state.
 */
class AlethiaCheckinHandler(private val panel: CheckinProjectPanel) : CheckinHandler() {
    private val LOG = Logger.getLogger(AlethiaCheckinHandler::class.java.name)

    /**
     * Called after a commit succeeds.
     * Writes queued flags from session state as git notes
     * on the new HEAD commit, then clears the queue.
     */
    override fun checkinSuccessful() {
        LOG.info("AlethiaCheckinHandler: checkinSuccessful fired")
        val project = panel.project
        val sessionState = project.service<AlethiaStateService>()

        // Get all repos in current project
        val repos = GitRepositoryManager.getInstance(project).repositories
        if (repos.isEmpty()) {
            LOG.warning("AlethiaCheckinHandler: no git repos found -> skipping note write")
            return
        }

        // For each repo get the current head SHA and check if any changes have occurred since last commit.
        repos.forEach { repo ->
            val repoPath = repo.root.path

            // Get the current HEAD SHA hash, this is the commit we just made
            val sha = repo.currentRevision ?: run {
                LOG.warning("AlethiaCheckinHandler: could not get HEAD SHA for $repoPath")
                return@forEach
            }

            // Skip writing step if nothing to write
            if (sessionState.flagCount() == 0) {
                LOG.info("AlethiaCheckinHandler: no flags queued for commit ${sha.take(7)} -> skipping")
                return@forEach
            }

            // Write the note
            writeGitNote(repoPath, sha, sessionState)
        }
    }

    /**
     * Saves a copy of alethia-state.xml before clearing the session state.
     * Name contains first 10 characetrs of the commit SHA.
     * Stored in .alethia/snapshots/ in repo root.
     *
     * @param repoPath  Absolute path to the repo
     * @param sha       The commit SHA these flags belong to
     */
    private fun saveSnapshot(repoPath: String, sha: String) {
        try {
            LOG.info("AlethiaCheckinHandler: saveSnapshot called for ${sha.take(10)}")

            // Retrieve the state file
            val stateFile = File(repoPath, ".idea/alethia-state.xml")
            if (!stateFile.exists()) {
                LOG.warning("AlethiaCheckinHandler: could not find alethia-state.xml — skipping snapshot")
                return
            }

            // Create the snapshot directory
            val snapshotDir = File(repoPath, ".alethia/snapshots")
            snapshotDir.mkdirs()

            // Copy state file with SHA in name
            val snapshotFile = File(snapshotDir, "alethia-state-${sha.take(10)}.xml")
            stateFile.copyTo(snapshotFile, overwrite = true)

            LOG.info("AlethiaCheckinHandler: snapshot saved -> ${snapshotFile.name}")
        } catch (e: Exception) {
            LOG.warning("AlethiaCheckinHandler: failed to save snapshot: ${e.message}")
        }
    }

    /**
     * Serializes queued flags to JSON and writes as a git note
     * on the given commit SHA using refs/notes/alethia.
     * Uses a temp file to avoid cross-platform shell escaping issues.
     * Clears session state after successful write.
     *
     * @param repoPath      Absolute path to the repo root
     * @param sha           The commit SHA to attach the note to
     * @param sessionState  The current session state containing queued flags
     */
    private fun writeGitNote(repoPath: String, sha: String, sessionState: AlethiaStateService) {
        try {
            // Retrieve all queued flags
            val newFlags = sessionState.getFlags()
            // Read existing flags
            val existingFlags = readExistingFlags(repoPath, sha)
            // All flags
            val allFlags = existingFlags + newFlags

            // Build the JSON block that will be saved in the note
            val noteContent = buildNoteJson(allFlags)
            // Create a temp file, avoids Windows Shell quote handling issues (Occurred in Hackathon)
            val tempFile = File(repoPath, ".git/alethia_temp_note.json")
            tempFile.writeText(noteContent)

            // Attach note to commit SHA under refs/notes/alethia
            val exitCode = ProcessBuilder(
                "git", "notes",
                "--ref=refs/notes/alethia",
                "add", "-f",
                "-F", tempFile.absolutePath,
                sha
            )
                .directory(File(repoPath))
                .start()
                .waitFor()
            // Remove temp file, no longer needed!
            tempFile.delete()

            // Log outcome and work done
            if (exitCode == 0) {
                LOG.info("AlethiaCheckinHandler: note written for commit ${sha.take(7)}, ${allFlags.size} flag(s)")
                saveSnapshot(repoPath, sha);
                sessionState.clearFlags()
                sessionState.lastCommitSha = sha
            } else {
                LOG.warning("AlethiaCheckinHandler: git notes add failed with exit code $exitCode")
            }

        } catch (e: Exception) {
            LOG.warning("AlethiaCheckinHandler: failed to write git note: ${e.message}")
        }
    }

    /**
     * Reads any existing commit notes from previous commit
     * Deserializes JSON object vis GSON library and retrieves
     * the list of FlaggedRegions to reconstruct the list of Flags.
     *
     * @param repoPath  Absolute path the repo root directory
     * @param sha       The commit SHA
     *
     * @return          List of FlaggedRegions (empty if no previous notes were found)
     */
    private fun readExistingFlags(repoPath: String, sha: String): List<FlaggedRegion> {
        return try {
            // Create external process to fetch the head commit notes
            val process = ProcessBuilder(
                "git", "notes",
                "--ref=refs/notes/alethia",
                "show", sha
            )
                .directory(File(repoPath))
                .start()

            // Save process output in a variable
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            // Check if the output was empty
            if (output.isBlank()) return emptyList()

            // Create Gson Object
            val gson = com.google.gson.Gson()
            // Deserialize the retrieved JSON objects
            val root = gson.fromJson(output, com.google.gson.JsonObject::class.java)
            // Feth the alethia block
            val alethia = root.getAsJsonObject("alethia") ?: return emptyList()
            // Fetch the flagged regions list from the alethia block
            val regions = alethia.getAsJsonArray("flaggedRegions") ?: return emptyList()
            // Map the Flag JSON objects to FlaggedRegion objects
            regions.map { element ->
                val obj = element.asJsonObject
                FlaggedRegion(
                    eventType = obj.get("eventType")?.asString ?: "",
                    file = obj.get("file")?.asString ?: "",
                    startLine = obj.get("startLine")?.asInt ?: 0,
                    endLine = obj.get("endLine")?.asInt ?: 0,
                    charCount = obj.get("charCount")?.asInt ?: 0,
                    rationale = obj.get("rationale")?.asString ?: "",
                    timeStamp = obj.get("timeStamp")?.asString ?: ""
                )
            }
        } catch (e: Exception) {
            LOG.warning("AlethiaCheckinHandler: could not parse existing note: ${e.message}")
            emptyList()
        }
    }

    /**
     * Builds the JSON block for the git note.
     * Commit SHA is intentionally excluded, git already knows which
     * commit the note is attached to, and a stored SHA becomes
     * incorrect if the commit is later rebased or amended.
     *
     * @param flags     The list of flagged regions to serialize
     * @return JSON     string to be written as the git note content
     */
    private fun buildNoteJson(flags: List<FlaggedRegion>): String {
        // Create the builder for JSON objects
        val gson = GsonBuilder().setPrettyPrinting().create()
        // Json object format
        val note = mapOf(
            "alethia"               to mapOf(
                "version"           to "0.1.0",
                "generatedAt"       to java.time.Instant.now().toString(),
                "flagCount"         to flags.size,
                "flaggedRegions"    to flags.map { flag ->
                    mapOf(
                        "eventType" to flag.eventType,
                        "file"      to flag.file,
                        "startLine" to flag.startLine,
                        "endLine"   to flag.endLine,
                        "charCount" to flag.charCount,
                        "rationale" to flag.rationale,
                        "timeStamp" to flag.timeStamp
                    )
                }
            )
        )
        return gson.toJson(note)
    }
}