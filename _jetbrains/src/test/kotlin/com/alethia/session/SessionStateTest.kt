package com.alethia.session

import com.alethia.model.FlaggedRegion
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase


/**
 * Integration tests for AlethiaStateService using BasePlatformTestCase.
 * Requires the IntelliJ Platform to run since AlethiaStateService is a
 * project-scoped service managed by the IntelliJ container, it cannot
 * be instantiated without a running platform.
 *
 * Tests cover flag addition, retrieval, clearing, snapshot isolation,
 * and lastCommitSha persistence.
 */
class SessionStateTest : BasePlatformTestCase() {

    // Create test variables
    private lateinit var stateService: AlethiaStateService

    // Executes right before each test
    override fun setUp() {
        super.setUp();
        stateService = project.service<AlethiaStateService>()
        stateService.clearFlags()
        stateService.lastCommitSha = null
    }

    // Executes right after each test
    override fun tearDown() {
        stateService.clearFlags()
        stateService.lastCommitSha = null
        super.tearDown()
    }

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

        assertEquals(1, snapshot.size)
        assertEquals(2, stateService.flagCount())
    }

    // ---------------------------  LASTCOMMITSHA METHODS  ----------------------------

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

    // ---------------------------  HELPER METHODS  ----------------------------

    private fun buildFlag(filePath: String) = FlaggedRegion(
        eventType = "EVENT_TYPE",
        file = filePath,
        startLine = 1,
        endLine = 10,
        charCount = 500,
        rationale = "Large clipboard paste - 500 chars",
        timeStamp = "2026-01-01T00:00:00Z"
    )
}