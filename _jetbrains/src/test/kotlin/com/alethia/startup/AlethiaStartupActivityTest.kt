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
 * setUp() calls installTest() which bypasses GitRepositoryManager and
 * runs the installer functions directly against repoPath, this gets used
 * for tests that verify installation outcomes without requiring VCS mappings.
 *
 * Tests cover git hook installation, git config setup, and idempotency
 * of repeated installations.
 */
class AlethiaStartupActivityTest : AlethiaBasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // calls installGitHook(), installGitCOnfig(), and installGitIgnore without
        // repoPath. This means we can bypass having to register the VCS root which consumes
        // more time and may result ie exceptions being thrown.
        AlethiaInstaller.installTest(repoPath)
    }

    // --------------------  HOOK TESTS  ---------------------

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

    fun `test installer skips setup when no git repos found`() {
        // Remove hook created in setUp
        File(repoPath, ".git/hooks/pre-push").delete()

        // install() uses GitRepositoryManager, this means empty repos
        AlethiaInstaller.install(project)

        // Hook should still not exist since installer skipped
        assertFalse(File(repoPath, ".git/hooks/pre-push").exists())
    }

    fun `test installer appends to existing pre-push hook`() {
        // Replace the hook created in setUp with one that has no alethia marker
        val hookFile = File(repoPath, ".git/hooks/pre-push")
        hookFile.writeText("#!/bin/sh\n# existing hook content\n")

        AlethiaInstaller.installTest(repoPath)

        assertTrue(hookFile.readText().contains(AlethiaConstants.HOOK_MARKER))
        assertTrue(hookFile.readText().contains("existing hook content"))
    }

    // --------------------  GIT CONFIG TESTS  ---------------------

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

    fun `test installGitConfig handles invalid repo path gracefully`() {
        // Pass a non-existent path to trigger ProcessBuilder exception
        AlethiaInstaller.installTest("/non/existent/path/that/does/not/exist")
        // Should not throw, installer should catch the exception
        assertTrue(true)
    }

    // --------------------  GITIGNORE TESTS  ---------------------

    fun `test gitignore entries are added`() {
        val gitignore = File(repoPath, ".gitignore")
        // Ensure that the .gitignore file exists
        assertTrue(gitignore.exists())
        // Verify that the alethia state file is included in the .gitignore file
        assertTrue(gitignore.readText().contains(AlethiaConstants.STATE_FILE_PATH))
    }

    fun `test installGitIgnore skips when gitignore does not exist`() {
        // Delete the gitignore that setUp creates
        File(repoPath, ".gitignore").delete()
        AlethiaInstaller.installTest(repoPath)
        // gitignore still should not exist
        assertFalse(File(repoPath, ".gitignore").exists())
    }
}