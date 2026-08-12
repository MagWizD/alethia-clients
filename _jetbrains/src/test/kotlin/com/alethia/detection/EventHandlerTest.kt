package com.alethia.detection

import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource
import com.alethia.model.FlaggedRegion
import com.alethia.services.LoggingService
import com.alethia.session.SessionState
import java.util.logging.Logger
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test


/**
 * Test class evaulating results from AlethiaEventHandler.kt
 * handler file.
 */
class EventHandlerTest {

    // Create a mocked Logger
    private val mockLogger = object : LoggingService {
        override fun getLogger(name: String) = Logger.getLogger(name)
    }

    // Create a mock of a SessionState object for testing
    private val mockState = object : SessionState {
        val flagList = mutableListOf<FlaggedRegion>()
        override fun addFlag(flag: FlaggedRegion) {flagList.add(flag)}
        override fun getFlags() = flagList.toList()
        override fun flagCount() = flagList.size
        override fun clearFlags() { flagList.clear() }
        override var lastCommitSha: String? = null
    }

    private lateinit var handler: AlethiaEventHandler

    @Before
    fun setup() {
        mockState.clearFlags()
        handler = AlethiaEventHandler(mockState, mockLogger)
    }


    // --------------------  PASTE EVENT TESTS  ---------------------

    @Test
    fun `large clipboard paste creates a flag`() {
        handler.submit(buildEvent(charCount = 500, source = EventSource.CLIPBOARD_PASTE))
        assertEquals(1, mockState.flagCount())
    }

    @Test
    fun `small clipboard paste does not create a flag`() {
        handler.submit(buildEvent(charCount = 25, source = EventSource.CLIPBOARD_PASTE))
        assertEquals(0, mockState.flagCount())
    }


    // --------------------  DEDUPLICATION TESTS  ---------------------

    @Test
    fun `document change suppressed when paste just fired for same file`() {
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
        assertEquals(1, mockState.flagCount())
    }


    @Test
    fun `document event not suppressed for different files`() {

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
        assertEquals(2, mockState.flagCount())
    }

    @Test
    fun `flag rationale matches clipboard paste`() {
        handler.submit(buildEvent(charCount = 500, source = EventSource.CLIPBOARD_PASTE))
        val flag = mockState.getFlags().first()
        assertTrue(flag.rationale.contains("clipboard paste"))
    }


    @Test
    fun `flag rationale matches document change`() {
        handler.submit(buildEvent(charCount = 500, source = EventSource.DOCUMENT_CHANGE))
        val flag = mockState.getFlags().first()
        assertTrue(flag.rationale.contains("source unknown"))
    }


    // --------------------  PATH SCURBBING TESTS  ---------------------

    @Test
    fun `file path is scrubbed to repo relative path`() {
        handler.submit(buildEvent(
            charCount = 500,
            source = EventSource.CLIPBOARD_PASTE,
            filePath = "/project/src/TestFile.kt",
            repoRoot = "/project"
        ))
        val flag = mockState.getFlags().first()
        assertEquals("src/TestFile.kt", flag.file)
    }


    // -----------------------  HELPER FUNCTIONS  -------------------------

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