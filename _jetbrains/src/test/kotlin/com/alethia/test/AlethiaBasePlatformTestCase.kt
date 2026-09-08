package com.alethia.test

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
 * What it takes care of...
 * - Initialize a real git repository in the temp project  directory so
 *   git-dependent code like getRepoRoot() resolves correctly.
 * - Loads test-specific logging config to keep test output separate
 *   from production alethia.log
 * - Provides a clean AlethiaStateService instance before each test
 * - Exposes registerVcsRoot() for listener tests that need git4idea
 *   to recognize the temp directory as a mapped VCS root.
 */
abstract class AlethiaBasePlatformTestCase : BasePlatformTestCase() {

    protected lateinit var stateService: AlethiaStateService
    protected lateinit var repoPath: String
    private var originalMappings: List<VcsDirectoryMapping> = emptyList()

    /**
     * Initialize a real git repository in the temp project directory.
     * Creates the directory if it doesn't exist, runs git init, and
     * sets user config so git commands work correctly during tests.
     */
    private fun initializeGitRepo() {
        val dir = File(repoPath)
        if (!dir.exists()) {
            dir.mkdirs()  // ← create it if it doesn't exist
        }

        listOf(
            listOf("git.exe", "init"),
            listOf("git", "config", "user.email", AlethiaTestConstants.TEST_USER_EMAIL),
            listOf("git", "config", "user.name", AlethiaTestConstants.TEST_USER_NAME)
        ).forEach { cmd ->
            val process = ProcessBuilder(cmd)
                .directory(File(repoPath))
                .redirectErrorStream(true)
                .inheritIO()
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            println("${cmd.joinToString(" ")} → exit=$exitCode output=$output")
        }

        // Create the .gitignore file
        File(repoPath, ".gitignore").createNewFile()
    }


    /**
     * XXXX - NEEDS WORK -> Logs are still being written to alethia.log not alethia-test.log
     *
     * Load test logging configuration before the IntelliJ Platform
     * initializes, this should make sure that LoggingFactory's FileHandler
     * attaches to alethia-test.log rather than alethia.log.
     * It is a static object because otherwise if it were a function
     * called from setUp(), it would be called after the platform and
     * LoggingFactory are already initialized.
     */
    companion object {
        init {
            // Prevent the JUL from searching for default logging property file
            System.clearProperty("java.util.logging.config.file")
            // Search for the logging-test.properties file within the project
            val configStream = AlethiaBasePlatformTestCase::class.java
                .getResourceAsStream("/logging-test.properties")
            // If the stream is not empty, then...
            if (configStream != null) {
                // ...reset logger configuration...
                java.util.logging.LogManager.getLogManager().reset()
                // ...and load the test-logger configuration
                java.util.logging.LogManager.getLogManager().readConfiguration(configStream)
                // Make sure you close the stream!!
                configStream.close()
            }
        }
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
     * Sets repoPath from the platform-provided temp project directory,
     * initializes the git repo and logging config, then provides a clean
     * AlethiaStateService with no flags and no lastCommitSha.
     */
    override fun setUp() {
//        System.err.println("=== AlethiaBasePlatformTestCase.setUp START ===")
        super.setUp()
//        System.err.println("=== after super.setUp ===")
//        println("=== project.basePath = ${project.basePath} ===")
        repoPath = project.basePath!!
//        println("=== repoPath set to: $repoPath ===")

        initializeGitRepo()

        stateService = project.service<AlethiaStateService>()
        stateService.clearFlags()
        stateService.lastCommitSha = null
    }

    /**
     * Runs after each test.
     * Restores VCS directory mappings, clears session state,
     * then delegates to BasePlatformTestCase for platform cleanup.
     */
    override fun tearDown() {
        ProjectLevelVcsManager.getInstance(project)
            .setDirectoryMappings(originalMappings)
        stateService.clearFlags()
        stateService.lastCommitSha = null
        super.tearDown()
    }

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
}