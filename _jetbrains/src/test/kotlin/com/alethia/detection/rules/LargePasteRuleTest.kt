package com.alethia.detection.rules

import com.alethia.config.DetectionConfig
import com.alethia.model.DetectionEvent
import com.alethia.model.EventSource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Units tests for LargePasteRule - the first concrete DetectionRule implementation.
 * Does not require the IntelliJ Platform since rule evaluation is pure logic
 * with no platform dependencies.
 *
 * Tests cover threshold boundaries, ignore patterns, event source differentiation,
 * and custom config values.
 */
class LargePasteRuleTest {

    private lateinit var config: DetectionConfig
    private lateinit var rule: LargePasteRule

    @Before
    fun setup() {
        config = DetectionConfig()
        rule = LargePasteRule(config)
    }

    // ---------------------------  THRESHOLD TESTS  ----------------------------

    @Test
    fun test() {
        assertEquals(1,1)
    }
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

    @Test
    fun `flags large clipboard paste`() {
        val event = buildEvent(charCount = 500, source = EventSource.CLIPBOARD_PASTE)
        val result = rule.evaluate(event)
        assertNotNull(result)
        assertTrue(result?.eventType.equals("LARGE_PASTE"))
    }

    @Test
    fun `flags large document change`() {
        val event = buildEvent(charCount = 500, source = EventSource.DOCUMENT_CHANGE)
        val result = rule.evaluate(event)
        assertNotNull(result)
        assertTrue(result?.eventType.equals("LARGE_INSERTION"))
    }

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

    // ---------------------------  IGNORE PATTERN TESTS  ----------------------------

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

    // ---------------------------  HELPERS  ----------------------------

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