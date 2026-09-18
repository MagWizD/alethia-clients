package com.alethia.detection

import com.alethia.utils.*
import com.alethia.model.EventSource
import com.alethia.test.AlethiaBasePlatformTestCase
import com.intellij.openapi.components.service

/**
 * Integration tests for AlethiaEventHandler using BasePlatformTestCase.
 * Requires the IntelliJ Platform since AlethiaEventHandler and
 * AlethiaStateService are both project-scoped services managed by
 * the IntelliJ container.
 *
 * Tests cover paste detection, document change detection, source priority
 * deduplication, flag rationale content, and path scrubbing.
 */
class EventHandlerTest : AlethiaBasePlatformTestCase() {

    private lateinit var handler: AlethiaEventHandler

    // Executes right before each test
    override fun setUp() {
        super.setUp()
        handler = project.service<AlethiaEventHandler>()
    }


    //  ------------------------------  PASTE EVENT TESTS  ----------------------------------
    
    fun `test large clipboard paste creates a flag`() {
        handler.submit(buildEvent(charCount = 500, source = EventSource.CLIPBOARD_PASTE))
        assertEquals(1, stateService.flagCount())
    }

    
    fun `test small clipboard paste does not create a flag`() {
        handler.submit(buildEvent(charCount = 25, source = EventSource.CLIPBOARD_PASTE))
        assertEquals(0, stateService.flagCount())
    }

    fun `test DetectionEvent exposes elapsedMs`() {
        val event = buildEvent(charCount = 500, source = EventSource.CLIPBOARD_PASTE)
        assertEquals(100L, event.elapsedMs)
    }

    // -------------------------------  DEDUPLICATION TESTS  -----------------------------
    // AlethiaEventHandler suppresses DOCUMENT_CHANGE events that fire within PASTE_WINDOW_MS
    // of a CLIPBOARD_PASTE for the same file.

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

        // Only one flag (document change event was suppressed)
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

        // Two flags (no suppression occurred)
        assertEquals(2, stateService.flagCount())
    }

    // -------------------------------  RATIONALE / SOURCE TESTS  -----------------------------

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

    fun `test EventSource clipboard paste has higher priority than document change`() {
        assertTrue(EventSource.CLIPBOARD_PASTE.priority > EventSource.DOCUMENT_CHANGE.priority)
    }

    // --------------------  PATH SCRUBBING TESTS  ---------------------

    fun `test scrubPath removes repo root prefix`() {
        val result = scrubPath("/project/src/main.kt", "/project")
        assertEquals("src/main.kt", result)
    }

    fun `test getRepoRoot returns null for unknown file path`() {
        // Files outside a mapped git repo return null so the listener
        // can exit early without creating a flag
        val result = getRepoRoot(project, "/nonexistent/path/file.kt")
        assertNull(result)
    }
}