package com.alethia.detection

import com.alethia.model.DetectionEvent
import com.alethia.model.EventSource
import com.alethia.session.AlethiaStateService
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Integration tests for AlethiaEventHandler using BasePlatformTestCase.
 * Requires the IntelliJ Platform since AlethiaEventHandler and
 * AlethiaStateService are both project-scoped services managed by
 * the IntelliJ container.
 *
 * Tests cover paste detection, document change detection, source priority
 * deduplication, flag rationale content, and path scrubbing.
 */
class EventHandlerTest : BasePlatformTestCase() {

    private lateinit var stateService: AlethiaStateService
    private lateinit var handler: AlethiaEventHandler

    // Executes right before each test
    override fun setUp() {
        super.setUp()
        stateService = project.service<AlethiaStateService>()
        handler = project.service<AlethiaEventHandler>()
        stateService.clearFlags()
    }

    // Executes right after each test
    override fun tearDown() {
        stateService.clearFlags()
        super.tearDown()
    }

    // --------------------  PASTE EVENT TESTS  ---------------------
    
    fun `test large clipboard paste creates a flag`() {
        handler.submit(buildEvent(charCount = 500, source = EventSource.CLIPBOARD_PASTE))
        assertEquals(1, stateService.flagCount())
    }

    
    fun `test small clipboard paste does not create a flag`() {
        handler.submit(buildEvent(charCount = 25, source = EventSource.CLIPBOARD_PASTE))
        assertEquals(0, stateService.flagCount())
    }

    // --------------------  DEDUPLICATION TESTS  ---------------------

    fun `test document change suppressed when paste just fired for same file`() {
        val filePath = "project/source/main.kt"

        // Handle the paste event first
        handler.submit(buildEvent(
            charCount = 500,
            source = EventSource.CLIPBOARD_PASTE,
            filePath = filePath
        ))

        // Document change fires immediately after for the same file
        handler.submit(buildEvent(
            charCount = 500,
            source = EventSource.DOCUMENT_CHANGE,
            filePath = filePath
        ))

        // Verify that only flag was created (document change event was suppressed)
        assertEquals(1, stateService.flagCount())
    }

    fun `test document event not suppressed for different files`() {

        // Handle the paste event on FileA
        handler.submit(buildEvent(
            charCount = 500,
            source = EventSource.CLIPBOARD_PASTE,
            filePath = "/project/src/TestFileA.kt"
        ))

        // Handle document change event on FileB
        handler.submit(buildEvent(
            charCount = 500,
            source = EventSource.DOCUMENT_CHANGE,
            filePath = "/project/src/TestFileB.kt"
        ))

        // Verify that 2 flags were created (no suppression occurred)
        assertEquals(2, stateService.flagCount())
    }

    fun `test flag rationale matches clipboard paste`() {
        handler.submit(buildEvent(charCount = 500, source = EventSource.CLIPBOARD_PASTE))
        val flag = stateService.getFlags().first()
        assertTrue(flag.rationale.contains("clipboard paste"))
    }

    fun `test flag rationale matches document change`() {
        handler.submit(buildEvent(
            charCount = 500,
            source = EventSource.DOCUMENT_CHANGE,
            filePath = "/project/src/TestFileA.kt"))
        val flag = stateService.getFlags().first()
        assertTrue(flag.rationale.contains("source unknown"))
    }

    // --------------------  PATH SCRUBBING TESTS  ---------------------

    fun `test file path is scrubbed to repo relative path`() {
        handler.submit(buildEvent(
            charCount = 500,
            source = EventSource.CLIPBOARD_PASTE,
            filePath = "/project/src/TestFile.kt",
            repoRoot = "/project"
        ))
        val flag = stateService.getFlags().first()
        assertEquals("src/TestFile.kt", flag.file)
    }

    // -----------------------  HELPERS  -------------------------

    private fun buildEvent(
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
}