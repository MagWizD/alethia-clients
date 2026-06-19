package com.alethia.detection.rules

import com.alethia.config.DetectionConfig
import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test class evaulating results from LargePasteRule.kt
 * rule file, checking both "happy path" and edge cases.
 */
class LargePasteRuleTest {

    private lateinit var config: DetectionConfig
    private lateinit var rule: LargePasteRule

    @Before
    fun setup() {
        config = DetectionConfig()
        rule = LargePasteRule(config)
    }

    // Below threshold

    @Test
    fun `returns null for small clipboard paste`() {
        val event = buildEvent(charCount = 50, source = EventSource.CLIPBOARD_PASTE)
        assertNull(rule.evaluate(event))
    }

    @Test
    fun `returns null for small document change`() {
        val event = buildEvent(charCount = 50, source = EventSource.DOCUMENT_CHANGE)
        assertNull(rule.evaluate(event))
    }

    // Above threshold

    @Test
    fun `flags large clipboard paste`() {
        val event = buildEvent(charCount = 500, source = EventSource.CLIPBOARD_PASTE)
        val result = rule.evaluate(event)
        assertNotNull(result)
        assertTrue(result!!.contains("clipboard paste"))
    }

    @Test
    fun `flags large document change`() {
        val event = buildEvent(charCount = 500, source = EventSource.DOCUMENT_CHANGE)
        val result = rule.evaluate(event)
        assertNotNull(result)
        assertTrue(result!!.contains("source unknown"))
    }

    // Exactly at threshold

    @Test
    fun `does not flag insertion exactly at threshold`() {
        val event = buildEvent(charCount = 200, source = EventSource.CLIPBOARD_PASTE)
        assertNull(rule.evaluate(event))
    }

    @Test
    fun `flags insertion one above threshold`() {
        val event = buildEvent(charCount = 201, source = EventSource.CLIPBOARD_PASTE)
        assertNotNull(rule.evaluate(event))
    }

    // Ignore patterns

    @Test
    fun `ignores git internal files`() {
        val event = buildEvent(
            charCount = 500,
            source = EventSource.CLIPBOARD_PASTE,
            filePath = "/project/.git/objects/abc123"
        )
        assertNull(rule.evaluate(event))
    }

    @Test
    fun `does not ignore normal files`() {
        val event = buildEvent(
            charCount = 500,
            source = EventSource.CLIPBOARD_PASTE,
            filePath = "/project/src/main.kt"
        )
        assertNotNull(rule.evaluate(event))
    }

    // Custom config

    @Test
    fun `respects custom threshold from config`() {
        val customConfig = DetectionConfig(largePasteThreshold = 1000)
        val customRule = LargePasteRule(customConfig)

        val belowCustom = buildEvent(charCount = 500, source = EventSource.CLIPBOARD_PASTE)
        val aboveCustom = buildEvent(charCount = 1001, source = EventSource.CLIPBOARD_PASTE)

        assertNull(customRule.evaluate(belowCustom))
        assertNotNull(customRule.evaluate(aboveCustom))
    }

    // Helper

    private fun buildEvent(
        charCount: Int,
        source: EventSource,
        filePath: String = "/project/src/Main.kt"
    ) = DetectionEvent(
        filePath = filePath,
        charCount = charCount,
        startLine = 1,
        endLine = 1,
        elapsedMs = 100,
        source = source
    )
}