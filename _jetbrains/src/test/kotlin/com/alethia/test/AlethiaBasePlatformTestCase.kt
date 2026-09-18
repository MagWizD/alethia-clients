package com.alethia.test

import com.alethia.model.DetectionEvent
import com.alethia.model.EventSource
import com.alethia.model.FlaggedRegion
import com.alethia.session.AlethiaStateService
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Base class for all Alethia integration tests requiring the IntelliJ Platform.
 * Extends BasePlatformTestCase to provide a real IntelliJ environment including
 * a headless project, service container, and VCS system.
 *
 * Note: BasePlatformTestCase reuses the same temp directory across all
 * test classes in a single run, so git init runs before every test to
 * ensure the repo is always present regardless of execution order
 */
abstract class AlethiaBasePlatformTestCase : BasePlatformTestCase() {

    protected lateinit var stateService: AlethiaStateService
    protected lateinit var repoPath: String
    // Stores VSC mappings before registerVCSRoot() so tearDown can restore them.
    private var originalMappings: List<VcsDirectoryMapping> = emptyList()


    /**
     * Initialize a real git repository in the temp project directory.
     * Creates the directory if it doesn't exist, runs git init, and
     * sets user config so git commands work correctly during tests.
     */
    private fun initializeGitRepo() {
        val dir = File(repoPath)
        if (!dir.exists()) dir.mkdirs()  // ← create it if it doesn't exist

        listOf(
            listOf("git.exe", "init"),
            listOf("git", "config", "user.email", AlethiaTestConstants.TEST_USER_EMAIL),
            listOf("git", "config", "user.name", AlethiaTestConstants.TEST_USER_NAME)
        ).forEach { cmd ->
            ProcessBuilder(cmd)
                .directory(File(repoPath))
                .redirectErrorStream(true)
                .inheritIO()
                .start()
                .waitFor()
        }

        // Create the .gitignore file
        File(repoPath, ".gitignore").createNewFile()
    }

    /**
     * Registers temp repo directory as a Git VCS root in IntelliJ's
     * project model. Required for listener tests where getRepoRoot()
     * needs git4idea to have the directory mapped.
     * Saves the original mappings so tearDown can restore them cleanly.
     * Only call this in test classes that need VCS root mapping.
     */
    protected fun registerVcsRoot() {
        val vcsManager = ProjectLevelVcsManager.getInstance(project)
        originalMappings = vcsManager.directoryMappings
        vcsManager.setDirectoryMappings(listOf(
            VcsDirectoryMapping(repoPath, "Git")
        ))
    }

    /**
     * Runs before each test.
     * Gets repoPath from the platform temp directory, initializes the git
     * repo, and resets AlethiaStateService to a clean state.
     */
    override fun setUp() {
        super.setUp()
        repoPath = project.basePath!!
        initializeGitRepo()
        stateService = project.service<AlethiaStateService>()
        stateService.clearFlags()
        stateService.lastCommitSha = null
    }

    /**
     * Runs after each test.
     * Restores VCS mappings, clears session state, then delegates to
     * BasePlatformTestCase for platform cleanup.
     */
    override fun tearDown() {
        ProjectLevelVcsManager.getInstance(project)
            .setDirectoryMappings(originalMappings)
        stateService.clearFlags()
        stateService.lastCommitSha = null
        super.tearDown()
    }

    //  ----------------------  HELPER METHODS  -------------------------

    /**
     * Create a real file on disk inside the temp git repository.
     * Use this instead of myFixture.configureByTest() when the test
     * needs the file path to resolve against repoPath -> myFixture
     * creates files in an in-memory virtual filesystem whose paths
     * do not match up with the temp directory and will fail checks.
     */
    protected fun createRepoFile(name: String): VirtualFile {
        val repoDir = LocalFileSystem.getInstance()
            .refreshAndFindFileByPath(repoPath)!!
        return WriteCommandAction.runWriteCommandAction<VirtualFile>(project) {
            repoDir.createChildData(this, name)
        }
    }

    /**
     * Builds FlaggedRegion with sensible test defaults.
     * Override individual fields as needed per test.
     */
    protected fun buildFlag(filePath: String) = FlaggedRegion(
        eventType = "EVENT_TYPE",
        file = filePath,
        startLine = 1,
        endLine = 10,
        charCount = 500,
        rationale = "Large clipboard paste - 500 chars",
        timeStamp = "2026-01-01T00:00:00Z"
    )

    /**
     * Builds a DetectionEvent with sensible test defaults.
     * Override charCount, source, filePath, or repoRoot as needed per test.
     */
    protected fun buildEvent(
        charCount: Int,
        source: EventSource,
        filePath: String = "/project/src/Main.kt",
        repoRoot: String = "/project"
    ) = DetectionEvent(
        filePath = filePath,
        repoRoot = repoRoot,
        charCount = charCount,
        startLine = 1,
        endLine = 1,
        elapsedMs = 100,
        source = source
    )

    /**
     * Creates a commit with the specified message.
     * Used for testing note creation.
     * Runs the git add and commit commands, returns the HEAD SHA
     * for reviewing generated flags.
     */
    protected fun makeCommit(message: String = "test commit"): String {
        // Unique filename ensures each commit has a distinct SHA
        File(repoPath, "test_${System.currentTimeMillis()}.kt")
            .writeText("fun main() {}")

        listOf(
            listOf("git", "add", "."),
            listOf("git", "commit", "-m", message)
        ).forEach { cmd ->
            ProcessBuilder(cmd)
                .directory(File(repoPath))
                .start()
                .waitFor()
        }

        // Return the HEAD SHA
        val process = ProcessBuilder("git", "rev-parse", "HEAD")
            .directory(File(repoPath))
            .start()
        val sha = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        return sha
    }
}