package com.alethia.session

import com.alethia.model.FlaggedRegion
import com.alethia.test.AlethiaBasePlatformTestCase

/**
 * Integration tests for AlethiaStateService using BasePlatformTestCase.
 * Requires the IntelliJ Platform to run since AlethiaStateService is a
 * project-scoped service managed by the IntelliJ container, it cannot
 * be instantiated without a running platform.
 *
 * Tests cover flag addition, retrieval, clearing, snapshot isolation,
 * and lastCommitSha persistence.
 */
class SessionStateTest : AlethiaBasePlatformTestCase() {

    // ---------------------------  ADDITION / RETRIEVAL TESTS  ----------------------------

    fun `test starts with no flags`() {
        assertEquals(0, stateService.flagCount())
    }

    fun `test adds a flag correctly`() {
        stateService.addFlag(buildFlag("/project/src/testFileA.kt"))
        assertEquals(1, stateService.flagCount())
    }

    fun `test adds multiple flags correctly`() {
        stateService.addFlag(buildFlag("/project/src/testFileA.kt"))
        stateService.addFlag(buildFlag("/project/src/testFileB.kt"))
        stateService.addFlag(buildFlag("/project/src/testFileC.kt"))
        assertEquals(3, stateService.flagCount())
    }

    fun `test getFlags returns all added flags`() {
        val flag1 = buildFlag("/project/src/testFileA.kt")
        val flag2 = buildFlag("/project/src/testFileB.kt")

        stateService.addFlag(flag1)
        stateService.addFlag(flag2)

        val flags = stateService.getFlags()

        assertEquals(2, flags.size)
        assertTrue(flags.contains(flag1))
        assertTrue(flags.contains(flag2))
    }


    // ---------------------------  CLEAR TESTS  ----------------------------

    fun `test clearFlags removes all queued flags`() {
        stateService.addFlag(buildFlag("/project/src/testFileA.kt"))
        stateService.addFlag(buildFlag("/project/src/testFileB.kt"))
        stateService.addFlag(buildFlag("/project/src/testFileC.kt"))
        stateService.addFlag(buildFlag("/project/src/testFileD.kt"))

        stateService.clearFlags()

        assertEquals(0, stateService.flagCount())
    }

    fun `test able to add flags after clearing`() {
        stateService.addFlag(buildFlag("/project/src/testFileA.kt"))
        stateService.addFlag(buildFlag("/project/src/testFileB.kt"))

        stateService.clearFlags()

        stateService.addFlag(buildFlag("/project/src/testFileA.kt"))
        stateService.addFlag(buildFlag("/project/src/testFileB.kt"))
        stateService.addFlag(buildFlag("/project/src/testFileC.kt"))

        assertEquals(3, stateService.flagCount())
    }

    // ---------------------------  SNAPSHOT TESTS  ----------------------------

    fun `test getFlags returns a values snapshot not a reference`() {
        stateService.addFlag(buildFlag("/project/src/testFileA.kt"))
        val snapshot = stateService.getFlags()
        stateService.addFlag(buildFlag("/project/src/testFileB.kt"))

        // Snapshot should reflect state at time of capture
        assertEquals(1, snapshot.size)
        assertEquals(2, stateService.flagCount())
    }

    // ---------------------------  LASTCOMMITSHA  ----------------------------

    fun `test lastCommitSha is null by default`() {
        assertNull(stateService.lastCommitSha)
    }

    fun `test lastCommitSha can be set and retrieved`() {
        stateService.lastCommitSha = "abc123"
        assertEquals("abc123", stateService.lastCommitSha)
    }

    fun `test lastCommitSha can be updated`() {
        stateService.lastCommitSha = "abc123"
        stateService.lastCommitSha = "def456"
        assertEquals("def456", stateService.lastCommitSha)
    }

    // ---------------------------  STATE LIFECYCLE  ----------------------------

    fun `test getState returns current state`() {
        stateService.addFlag(buildFlag("/project/src/test.kt"))
        val state = stateService.state
        assertEquals(1, state.flaggedRegions.size)
    }

    fun `test loadState restores state`() {
        val newState = AlethiaStateService.State()
        newState.flaggedRegions.add(buildFlag("/project/src/test.kt"))
        stateService.loadState(newState)
        assertEquals(1, stateService.flagCount())
    }

    // ---------------------------  DESERIALIZATION  ----------------------------

    fun `test FlaggedRegionAdapter deserializes correctly`() {
        // Instantiate a GSON builder with our custom type adapter
        val gson = com.google.gson.GsonBuilder()
            .registerTypeAdapter(FlaggedRegion::class.java, com.alethia.model.FlaggedRegionAdapter())
            .create()

        // Build a test json object.
        val json = """
            {
                "eventType":"LARGE_PASTE",
                "file":"src/test.kt",
                "startLine":1,
                "endLine":10,
                "charCount":500,
                "rationale":"test",
                "timeStamp":"2026-01-01T00:00:00Z"
            }""".trimIndent()

        // Translate the Json object to a FlaggedRegion
        val region = gson.fromJson(json, FlaggedRegion::class.java)

        // Verify the deserialization was correct
        assertEquals("LARGE_PASTE", region.eventType)
        assertEquals("src/test.kt", region.file)
        assertEquals(1, region.startLine)
        assertEquals(10, region.endLine)
        assertEquals(500, region.charCount)
        assertEquals("test", region.rationale)
        assertEquals("2026-01-01T00:00:00Z", region.timeStamp)
    }
}