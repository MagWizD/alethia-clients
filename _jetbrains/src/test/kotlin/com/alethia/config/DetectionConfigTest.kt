package com.alethia.config

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test for DetectionConfig default and custom values.
 * Does not require the intelliJ Platform since DetectionConfig
 * is a plain data class with no platform dependencies.
 *
 * Tests cover default threshold values and custom overrides.
 */
class DetectionConfigTest {

    @Test
    fun test() {
        assertEquals(1,1)
    }

    @Test
    fun `default threshold is 200`() {
        val config = DetectionConfig()
        assertEquals(200, config.largePasteThreshold)
    }

    @Test
    fun `default debounce window is 2000ms`() {
        val config = DetectionConfig()
        assertEquals(2000L, config.debounceWindowMs)
    }

    @Test
    fun `default ignore patterns contains git`() {
        val config = DetectionConfig()
        assertTrue(config.ignorePatterns.contains(".git"))
    }

    @Test
    fun `custom threshold is applied`() {
        val config = DetectionConfig(largePasteThreshold = 500)
        assertEquals(500, config.largePasteThreshold)
    }
}