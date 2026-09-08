package com.alethia.startup

import com.alethia.config.AlethiaConstants
import com.alethia.test.AlethiaBasePlatformTestCase
import java.io.File

/**
 * Integration tests for AlethiaStartupActivity and AlethiaInstaller
 * using BasePlatformTestCase.
 * Requires the IntelliJ Platform since the startup activity depends
 * on GitRepositoryManager and project services.
 *
 * Tests cover git hook installation, git config setup, and idempotency
 * of repeated installations.
 */
class AlethiaStartupActivityTest : AlethiaBasePlatformTestCase() {

    // Runs before each
    override fun setUp() {
        super.setUp()
        AlethiaInstaller.installTest(repoPath)
    }

    // --------------------  HOOK TESTS  ---------------------
    // Evaluates the presence of the hook file, the alethia-managed
    // section, and that our managed block is appended only once.

    fun `test pre-push hook is installed on project open`() {
        val hookFile = File(repoPath, ".git/hooks/pre-push")
        assertTrue(hookFile.exists())
    }

    fun `test pre-push hook contains alethia marker`() {
        val hookFile = File(repoPath, ".git/hooks/pre-push")
        assertTrue(hookFile.readText().contains(AlethiaConstants.HOOK_MARKER))
    }

    fun `test pre-push hook is executable`() {
        val hookFile = File(repoPath, ".git/hooks/pre-push")
        assertTrue(hookFile.canExecute())
    }

    fun `test installer is idempotent`() {
        AlethiaInstaller.installTest(repoPath)   // Install a second time (first happened in setUp())
        val hookFile = File(repoPath, ".git/hooks/pre-push")
        val occurrences = hookFile.readText()
            .split(AlethiaConstants.HOOK_MARKER).size - 1
        assertEquals(1, occurrences)
    }

    // --------------------  GIT CONFIG TESTS  ---------------------
    // Check if all expected config values are present in the .git/config file


    fun `test git config rewriteRef is set`() {
        val config = File(repoPath, ".git/config").readText()
        assertTrue(config.contains(AlethiaConstants.NOTES_REF))
    }

    fun `test git config rebase is set`() {
        val config = File(repoPath, ".git/config").readText()
        assertTrue(config.contains("rebase = true"))
    }

    fun `test git config amend is set`() {
        val config = File(repoPath, ".git/config").readText()
        assertTrue(config.contains("amend = true"))
    }

    // --------------------  GITIGNORE TESTS  ---------------------


    fun `test gitignore entries are added`() {
        val gitignore = File(repoPath, ".gitignore")
        // Ensure that the .gitignore file exists
        assertTrue(gitignore.exists())
        // Verify that the alethia state file is included in the .gitignore file
        assertTrue(gitignore.readText().contains(AlethiaConstants.STATE_FILE_PATH))
    }
}