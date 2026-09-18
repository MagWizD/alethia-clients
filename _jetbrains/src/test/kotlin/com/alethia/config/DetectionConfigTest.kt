package com.alethia.config

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test for DetectionConfig and AlethiaConstants.
 * Does not require the IntelliJ Platform since there are
 * plain data classes and constants with no platform dependencies.
 *
 * Tests cover default threshold values and custom overrides.
 */
class DetectionConfigTest {

    //  ---------------------------  DEFAULT VALUE TESTS  -------------------------------

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

    //  ---------------------------  CUSTOM VALUE TESTS  -------------------------------

    @Test
    fun `custom threshold is applied`() {
        val config = DetectionConfig(largePasteThreshold = 500)
        assertEquals(500, config.largePasteThreshold)
    }

    //  ---------------------------  PLUGIN VERSION TEST  -------------------------------

    @Test
    fun `plugin version returns a value`() {
        // Returns the version from plugin.xml if running inside the platform,
        // or "unknown" as a fallback if the plugin ID is not recognized.
        // In either case the result should not be null or blank
        val version = AlethiaConstants.PLUGIN_VERSION
        assertNotNull(version)
        assertTrue(version.isNotBlank())
    }
}