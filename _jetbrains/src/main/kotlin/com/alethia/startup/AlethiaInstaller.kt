package com.alethia.startup

import java.util.logging.Logger
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager
import java.io.File


/**
 * Handles all one-time activiites when Alethia activates on a repo.
 * Called by AlethiaStartupActivity when a project opens.
 * All operations are idempotent, meaning it is safe to run on every project
 * open since each step checks if it has already been applied before.
 *
 * Duties:
 *  - Installs pre-push hook: Ensures notes are pushed to remote on every branch
 *  - Set git configs - ensures notes survive rebase and amendments
 */
object AlethiaInstaller {

    private val LOG = Logger.getLogger(AlethiaInstaller::class.java.name)

    /**
     * Entry point function - runs setup for all necessary installations.
     *
     * @param project The currently open project
     */
    fun install(project: Project) {

        LOG.info("AlethiaInstaller: install called for project=${project.name}")  // ← add this line
        val repos = GitRepositoryManager.getInstance(project).repositories

        // Check that a repo exists
        if (repos.isEmpty()) {
            LOG.info("AlethiaInstaller: no git repos found - skipping setup")
            return
        }

        repos.forEach { repo ->
            val repoPath = repo.root.path
            LOG.info("AlethiaInstaller: setting up repo at $repoPath")
            // Set up the pre-push hook
            installGitHook(repoPath)
            // Set up the rebase git note persistence
            installGitConfig(repoPath)
        }
    }

    /**
     * Writes the pre-push hook to .git/hooks/pre-push.
     * If the hook exists, append the Alethia block to the end.
     * Uses a marker to prevent duplicate installations.
     *
     * What the hook does:
     * - Feth remote notes into a temp ref
     * - Merge notes with local to avoid conflicts
     * - Push the merged notes to remote repo
     *
     * We need to ensure that we never block the contributor's push.
     * We do this by using an OR to log failures and do not block.
     *
     * @param repoPath Absolute path to the repository root
     */
    private fun installGitHook(repoPath: String) {
        val hooksDir = File(repoPath, ".git/hooks")
        val hookFile = File(hooksDir, "pre-push")

        // Marker we use to set/find our managed section
        val alethiaMarker = "# alethia-managed"

        // The block of alethia configs for the pre-push hook
        val alethiaBlock = """
            
            $alethiaMarker
            
            # Fetch remote notes into temp ref
            git fetch origin refs/notes/alethia:refs/notes/aletheia-remote 2>/dev/null || \
                echo "[Alethia] Warning: could not fetch remote notes -> skipping merge"
            
            # Merge remote notes with local
            git notes --ref=refs/notes/alethia merge refs/notes/aletheia-remote 2>/dev/null || \
                echo "[Alethia] Warning: could not merge remote notes"
            
            # Push notes to remote
            git push origin refs/notes/alethia 2>/dev/null || \
                echo "[Alethia] Warning: could not push notes to remote, notes may not be visible to Themis"
        """.trimIndent()

        // Check for existing hook file
        if (hookFile.exists()) {
            val existing = hookFile.readText()
            // Check for pre-existing Alethia marker
            if (existing.contains(alethiaMarker)) {
                LOG.info("AlethiaInstaller: hook file already exists, skipping")
                return
            }
            // Append text to the hook file, log the changes
            hookFile.appendText("\n$alethiaBlock")
            LOG.info("AlethiaInstaller: appended Alethia Block to the existing pre-push hook")
        } else {
            // Create the Hooks directory if it doesn't already exist
            hooksDir.mkdirs()
            // Create a new file with the Alethia block
            hookFile.writeText("#!/bin/sh\n$alethiaBlock\nexit 0\n")
            // Make it executable
            hookFile.setExecutable(true)
            LOG.info("AlethiaInstaller: created pre-push hook file")
        }
    }

    /**
     * Set git config values in the repo's local .git/config.
     * Does not touch global settings set by developer
     *
     * Values touched:
     * - notes.rewriteRef       Tells git which notes ref to copy when rewriting commits
     * - notes.rewrite.rebase   Copies notes when rebasing
     * - notes.rewrite.amend    Copies notes when amending
     *
     * This ensures that notes will survive standard rebases and amends.
     * Interactive rebase mid-steps are a known limitation
     *
     * @param repoPath Absolute path to the repostiory root
     */
    private fun installGitConfig(repoPath: String) {
        // Create config mappings to enable note copying on rebase/amend
        val configs = mapOf(
            // Tell git which notes to copy when commits are rewritten
            "notes.rewriteRef" to "refs/notes/alethia",
            // Copy notes automatically when git rebase occurs
            "notes.rewrite.rebase" to "true",
            // Copy notes automatically when git amend occurs
            "notes.rewrite.amend" to "true"
        )

        // For each config above, create a process outside of the JVM to udpate the git config
        configs.forEach { (key, value) ->
            try {
                val result = ProcessBuilder("git", "config", key, value)
                    .directory(File(repoPath))
                    .start()
                    .waitFor()

                if (result == 0) {
                    LOG.info("AlethiaInstaller: set $key = $value")
                } else {
                    // Non-zero means git failed
                    // Log the warning and continue
                    LOG.warning("AlethiaInstaller: failed to set $key - git config returned $result")
                }
            } catch (e: Exception) {
                // Error here means that the process couldnt start.
                // git may not be installed or accessible on PATH
                // Log and continue, dont let this crash the startup activity
                LOG.warning("AlethiaInstaller: error setting $key -  ${e.message}")
            }
        }
    }
}