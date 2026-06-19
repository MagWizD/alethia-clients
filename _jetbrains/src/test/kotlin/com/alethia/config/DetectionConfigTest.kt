package com.alethia.config

import org.junit.Assert.*
import org.junit.Test

/**
 * Test class evaluating if default config values are
 * correctly being accessed and set.
 */
class DetectionConfigTest {

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