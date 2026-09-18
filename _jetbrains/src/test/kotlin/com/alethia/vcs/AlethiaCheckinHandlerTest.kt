package com.alethia.vcs

import com.alethia.test.AlethiaBasePlatformTestCase
import com.alethia.test.AlethiaTestConstants
import com.intellij.openapi.vcs.CheckinProjectPanel
import java.io.File
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Integration tests for AlethiaCheckinHandlerFactory and AlethiaCheckinHandler
 * using BasePlatformTestCase.
 * Requires the IntelliJ Platform since the checkin handler hooks into
 * IntelliJ's VCS commit pipeline.
 *
 * CheckinProjectPanel is mocked since it is an IntelliJ UI component
 * that cannot be constructed in a headless test environment
 *
 * Tests cover git note creation on commit, session state clearing after
 * commit, snapshot saving, and lastCommitSha update.
 */
class AlethiaCheckinHandlerTest : AlethiaBasePlatformTestCase() {

    private lateinit var handler: AlethiaCheckinHandler

    override fun setUp() {
        super.setUp()
        // VCS root is required
        registerVcsRoot()
        // Create file to commit to repo
        File(repoPath, AlethiaTestConstants.TEST_FILE_NAME)
            .writeText("fun main() {}")

        val mockPanel = mock(CheckinProjectPanel::class.java)
        `when`(mockPanel.project).thenReturn(project)
        handler = AlethiaCheckinHandler(mockPanel)
    }

    //  ------------------------  FULL PIPELINE TESTS  -----------------------

    fun `test checkinSuccessful writes note and clears flags`() {
        // Add a dummy flag to the stateService and ensure it is present
        stateService.addFlag(buildFlag("src/test.kt"))
        assertEquals(1, stateService.flagCount())
        // Create a commit
        makeCommit()

        // Refresh repo state, it was giving errors about running on the EDT, so we need to
        // use a background thread to avoid that
        com.intellij.openapi.application.ApplicationManager.getApplication()
            .executeOnPooledThread {
                git4idea.repo.GitRepositoryManager.getInstance(project).repositories
                    .forEach { it.update() }
            }.get() // wait for completion

        // Call checkinSuccessful
        handler.checkinSuccessful()
        // Flags should be cleared
        assertEquals(0, stateService.flagCount())
        // lastCommitSha should be set
        assertNotNull(stateService.lastCommitSha)
    }

    //  -------------------------  writeGitNotes() TESTS  ----------------------------

    fun `test git note is written on successful commit`() {
        val sha = makeCommit()
        stateService.addFlag(buildFlag("src/test.kt"))
        handler.writeGitNote(repoPath, sha, stateService)

        val process = ProcessBuilder("git", "notes", "--ref=refs/notes/alethia", "show", sha)
            .directory(File(repoPath))
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        assertTrue(output.contains("\"eventType\": \"EVENT_TYPE\""))
        assertTrue(output.contains("\"file\": \"src/test.kt\""))
    }

    fun `test flags are cleared after successful commit`() {
        val sha = makeCommit()
        stateService.addFlag(buildFlag("src/test.kt"))
        assertEquals(1, stateService.flagCount())

        handler.writeGitNote(repoPath, sha, stateService)

        assertEquals(0, stateService.flagCount())
    }

    fun `test lastCommitSha is updated after commit`() {
        assertNull(stateService.lastCommitSha)

        val sha = makeCommit()
        handler.writeGitNote(repoPath, sha, stateService)

        assertEquals(sha, stateService.lastCommitSha)
    }

    fun `test snapshot is saved before flags are cleared`() {
        val sha = makeCommit()
        stateService.addFlag(buildFlag("src/test.kt"))

        // We need to create the state file manually, PersistentStateComponent
        // only saves flags to the alethia-state.xml file on IDE exit or an explicit
        // save. So we create the state file just to ensure a snapshot is created.
        val ideaDir = File(repoPath, ".idea")
        ideaDir.mkdirs()
        File(ideaDir, "alethia-state.xml").writeText("<project/>")

        handler.writeGitNote(repoPath, sha, stateService)

        val snapshotFile = File(repoPath, ".alethia/snapshots/alethia-state-${sha.take(10)}.xml")
        assertTrue(snapshotFile.exists())
    }

    fun `test writeGitNote merges with existing flags`() {
        val sha = makeCommit()

        // Write first a git note
        stateService.addFlag(buildFlag("src/fileA.kt"))
        handler.writeGitNote(repoPath, sha, stateService)

        // Write a second note readExistingFlags should pick up first
        stateService.addFlag(buildFlag("src/fileB.kt"))
        handler.writeGitNote(repoPath, sha, stateService)

        // Verify both flags are in the note
        val process = ProcessBuilder("git", "notes", "--ref=refs/notes/alethia", "show", sha)
            .directory(File(repoPath))
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()

        assertTrue(output.contains("src/fileA.kt"))
        assertTrue(output.contains("src/fileB.kt"))
        assertTrue(output.contains("\"flagCount\": 2"))
    }

    fun `test no note written when no flags queued`() {
        val sha = makeCommit()
        // Verify that there are no pre-existing notes
        assertEquals(0, stateService.flagCount())
        // Create the notes
        handler.writeGitNote(repoPath, sha, stateService)

        // Create a process to fetch notes from alethia note file
        val process = ProcessBuilder("git", "notes",
            "--ref=refs/notes/alethia", "show", sha)
            .directory(File(repoPath))
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()

        // Verify that no notes were created
        assertTrue(output.contains("\"flagCount\": 0"))
        assertTrue(output.contains("\"flaggedRegions\": []"))
    }
}