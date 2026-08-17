package com.alethia.detection.listeners

import com.alethia.config.AlethiaConstants
import java.util.logging.Logger
import git4idea.push.GitPushListener
import git4idea.push.GitPushRepoResult
import git4idea.repo.GitRepository
import java.io.File

/**
 * Listens for git pushes events
 * Registered in plugin.xml as applicationListener.
 * On successful push, pushes Alethia notes to remote
 * under refs/notes/alethia.
 *
 */
class AlethiaGitPushListener : GitPushListener {

    private val LOG = Logger.getLogger(AlethiaGitPushListener::class.java.name)

    /**
     * Fires after every successful git push.
     * Only acts on successful pushes, skips failed pushes.
     *
     * @param repository    The repo that was pushed
     * @param pushResult    The result of push operation
     */
    override fun onCompleted(
        repository: GitRepository,
        pushResult: GitPushRepoResult
    ) {
        // Only push notes if the main push was succeessful
        if (pushResult.type != GitPushRepoResult.Type.SUCCESS) {
            LOG.info("AlethiaGitPushListener: push was not successful (${pushResult.type}) -> skipping notes push")
            return
        }

        // Push was successful! Log it!
        val repoPath = repository.root.path
        LOG.info("AlethiaGitPushListener: push succeeded -> pushing notes for $repoPath")
        pushNotes(repoPath)
    }

    /**
     * Tasked with pushing Alethia notes to remote under refs/notes/alethia.
     * Merges remote notes first to avoid conflicts
     * from multiple contributors writing local notes.
     *
     * @param repoPath  Absolute path to the repo root
     */
    private fun pushNotes(repoPath: String) {
        val dir = File(repoPath)

        // Step 1: Fetch the remote notes into temp ref
        // Use temporary namespace for remote notes before merging them
        val fetchExit = ProcessBuilder(
            "git", "fetch", "origin",
            "refs/notes/alethia:${AlethiaConstants.NOTES_REMOTE_REF}",
        )
            .directory(dir)
            .start()
            .waitFor()

        if (fetchExit != 0) {
            LOG.warning("AlethiaGitPushListener: failed to fetch remote notes, going forward with push anyway")
        }

        // Step 2: Merge the remote notes with the local notes
        // Build a process to run the `git notes` command
        val mergeExit = ProcessBuilder(
            "git",
            "notes",
            "--ref=${AlethiaConstants.NOTES_REF}",
            "merge",
            AlethiaConstants.NOTES_REMOTE_REF
        )
            .directory(dir)
            .start()
            .waitFor()

        if (mergeExit != 0) {
            LOG.warning("AlethiaGitPushListener: failed to merge remote notes -> going forward with push anyway")
        }

        // Step 3: Push the merged notes to remote
        val pushExit = ProcessBuilder(
            "git", "push", "origin",
            AlethiaConstants.NOTES_REF
        )
            .directory(dir)
            .start()
            .waitFor()

        // Check output for success/failure codes
        if (pushExit == 0) {
            // Success!
            LOG.info("AlethiaGitPushListener: notes pushed successfully")
        } else {
            // Failure!
            LOG.warning("AlethiaGitPushListener: failed to push notes: exit code $pushExit")
        }
    }
}