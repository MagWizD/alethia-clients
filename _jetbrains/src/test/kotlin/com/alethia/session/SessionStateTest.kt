package com.alethia.session

import com.alethia.model.FlaggedRegion

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 *
 */
class SessionStateTest {

    // Implement a simple SessionState for testing
    private val state = object : SessionState{
        val flagList = mutableListOf<FlaggedRegion>()
        override fun addFlag(flag: FlaggedRegion) {flagList.add(flag)}
        override fun getFlags() = flagList.toList()
        override fun flagCount() = flagList.size
        override fun clearFlags() {flagList.clear()}
        override var lastCommitSha: String? = null
    }

    @Before
    fun setup() {
        state.clearFlags()
    }

    // ---------------------------  ADDITION / RETRIEVAL TESTS  ----------------------------

    @Test
    fun `starts with no flags`() {
        assertEquals(0, state.flagCount())
    }

    @Test
    fun `adds a flag correctly`() {
        state.addFlag(buildFlag("/project/src/testFileA.kt"))
        assertEquals(1, state.flagCount())
    }

    @Test
    fun `adds multiple flags correctly`() {
        state.addFlag(buildFlag("/project/src/testFileA.kt"))
        state.addFlag(buildFlag("/project/src/testFileB.kt"))
        state.addFlag(buildFlag("/project/src/testFileC.kt"))
        assertEquals(3, state.flagCount())
    }

    @Test
    fun `getFlags returns all added flags`() {
        val flag1 = buildFlag("/project/src/testFileA.kt")
        val flag2 = buildFlag("/project/src/testFileB.kt")

        state.addFlag(flag1)
        state.addFlag(flag2)

        val flags = state.getFlags()

        assertEquals(2, flags.size)
        assertTrue(flags.contains(flag1))
        assertTrue(flags.contains(flag2))
    }

    // ---------------------------  CLEAR TESTS  ----------------------------

    @Test
    fun `clearFlags removes all queued flags`() {
        state.addFlag(buildFlag("/project/src/testFileA.kt"))
        state.addFlag(buildFlag("/project/src/testFileB.kt"))
        state.addFlag(buildFlag("/project/src/testFileC.kt"))
        state.addFlag(buildFlag("/project/src/testFileD.kt"))

        state.clearFlags()

        assertEquals(0, state.flagCount())
    }

    @Test
    fun `able to add flags after clearing`() {
        state.addFlag(buildFlag("/project/src/testFileA.kt"))
        state.addFlag(buildFlag("/project/src/testFileB.kt"))

        state.clearFlags()

        state.addFlag(buildFlag("/project/src/testFileA.kt"))
        state.addFlag(buildFlag("/project/src/testFileB.kt"))
        state.addFlag(buildFlag("/project/src/testFileC.kt"))

        assertEquals(3, state.flagCount())

    }

    // ---------------------------  SNAPSHOT TESTS  ----------------------------

    @Test
    fun `getFlags returns a values snapshot not a reference`() {
        state.addFlag(buildFlag("/project/src/testFileA.kt"))

        val snapshot = state.getFlags()

        state.addFlag(buildFlag("/project/src/testFileB.kt"))

        assertEquals(1, snapshot.size)
        assertEquals(2, state.flagCount())
    }

    // ---------------------------  LASTCOMMITSHA METHODS  ----------------------------

    @Test
    fun `lastCommitSha is null by default`() {
        assertNull(state.lastCommitSha)
    }

    @Test
    fun `lastCommitSha can be set and retrieved`() {
        state.lastCommitSha = "abc123"
        assertEquals("abc123", state.lastCommitSha)
    }

    @Test
    fun `lastCommitSha can be updated`() {
        state.lastCommitSha = "abc123"
        state.lastCommitSha = "def456"
        assertEquals("def456", state.lastCommitSha)
    }

    // ---------------------------  HELPER METHODS  ----------------------------

    private fun buildFlag(filePath: String) = FlaggedRegion(
        file = filePath,
        startLine = 1,
        endLine = 10,
        charCount = 500,
        rationale = "Large clipboard paste - 500 chars",
        timeStamp = "2026-01-01T00:00:00Z"
    )
}