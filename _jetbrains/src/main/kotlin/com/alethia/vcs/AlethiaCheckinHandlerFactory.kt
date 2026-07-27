package com.alethia.vcs

import com.alethia.model.FlaggedRegion
import com.alethia.session.AlethiaStateService
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.openapi.diagnostic.Logger
import git4idea.repo.GitRepositoryManager
import java.io.File

/**
 * Registers Alethia into IntelliJ's commit pipeline.
 * Called by IntelliJ every time a commit is made in any VCS.
 * Creates an AlethiaCheckinHandler per commit operation.
 */
class AlethiaCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel,
                               commitContext: CommitContext):
            CheckinHandler {
        return AlethiaCheckinHandler(panel)
    }
}

/**
 * Handles a single commit operation.
 * Never blocks or modifies the commit.
 * On successful commit, all queued flags
 * are serialized to git notes.
 */
class AlethiaCheckinHandler(private val panel: CheckinProjectPanel) :
    CheckinHandler() {

    private val LOG = Logger.getInstance(AlethiaCheckinHandler::class.java)

    /**
     * Called after a commit succeeds.
     * Writes queued flags from session state as git notes
     * on the new HEAD commit, then clears the queue.
     */
    override fun checkinSuccessful() {
        val project = panel.project
        val sessionState = project.service<AlethiaStateService>()

        // Get all repos in current project
        val repos = GitRepositoryManager.getInstance(project).repositories
        if (repos.isEmpty()) {
            LOG.warn("AlethiaCheckinHandler: no git repos found -> skipping note write")
            return
        }

        // For each repo get the current head SHA and check if any changes have occurred since last commit.
        repos.forEach { repo ->
            val repoPath = repo.root.path

            // Get the current HEAD SHA hash, this is the commit we just made
            val sha = repo.currentRevision ?: run {
                LOG.warn("AlethiaCheckinHandler: could not get HEAD SHA for $repoPath")
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
            val flags = sessionState.getFlags()
            // Build the JSON block that will be saved in the note
            val noteContent = buildNoteJson(flags)

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
                LOG.info("AlethiaCheckinHandler: note written for commit ${sha.take(7)}, ${flags.size} flag(s)")
                sessionState.clearFlags()
            } else {
                LOG.warn("AlethiaCheckinHandler: git notes add failed with exit code $exitCode")
            }

        } catch (e: Exception) {
            LOG.warn("AlethiaCheckinHandler: failed to write git note: ${e.message}")
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

        // Serialize each flag as a JSON object
        val flagsJson = flags.joinToString(",\n        ") { flag ->
            """
        {
            "file": "${flag.file}",
            "startLine": ${flag.startLine},
            "endLine": ${flag.endLine},
            "charCount": ${flag.charCount},
            "rationale": "${flag.rationale}",
            "timeStamp": "${flag.timeStamp}"
        }""".trimIndent()
        }

        // Wrap flags in the root note object with metadata
        return """
        {
            "alethiaVersion": "0.1.0",
            "generatedAt": "${java.time.Instant.now()}",
            "flagCount": ${flags.size},
            "flaggedRegions": [$flagsJson]
        }
    """.trimIndent()
    }
}