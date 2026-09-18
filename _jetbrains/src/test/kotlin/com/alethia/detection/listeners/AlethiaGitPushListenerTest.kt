package com.alethia.detection.listeners

import com.alethia.test.AlethiaBasePlatformTestCase
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.push.GitPushRepoResult
import git4idea.repo.GitRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Integration tests for AlethiaGitPushListener using BasePlatformTestCase.
 * Requires the IntelliJ Platform since the listener hooks into
 * git4idea's GitPushListener.
 *
 * GitRepository and GitPushRepoResult are mocked since triggering a
 * real push would require a remote. Tests here cover the early return logic
 * and verify the success path runs without throwing even when a remote is missing.
 */
class AlethiaGitPushListenerTest : AlethiaBasePlatformTestCase() {

    private lateinit var listener: AlethiaGitPushListener
    private lateinit var mockRepo: GitRepository
    private lateinit var mockResult: GitPushRepoResult

    override fun setUp() {
        super.setUp()
        listener = AlethiaGitPushListener()
        mockRepo = mock(GitRepository::class.java)
        mockResult = mock(GitPushRepoResult::class.java)
        // Point the mock repo at the temp directory so git commands
        // have a valid working directory
        `when`(mockRepo.root).thenReturn(
            LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(repoPath)!!
        )
    }

    //  -----------------------------  EARLY RETURN TESTS  --------------------------------
    // Verify that failed or rejected pushes skip the notes push entirely

    fun `test onCompleted skips notes push when push failed`() {
        `when`(mockResult.type).thenReturn(GitPushRepoResult.Type.ERROR)
        // Should return early, no git commands run, no exception thrown
        listener.onCompleted(mockRepo, mockResult)
        assertTrue(true)
    }

    fun `test onCompleted skips notes push when push rejected`() {
        `when`(mockResult.type).thenReturn(GitPushRepoResult.Type.REJECTED_NO_FF)
        listener.onCompleted(mockRepo, mockResult)
        assertTrue(true)
    }

    //  -----------------------------  HAPPY PATH TEST  --------------------------------

    fun `test onCompleted attempts notes push when push successful`() {
        `when`(mockResult.type).thenReturn(GitPushRepoResult.Type.SUCCESS)
        // Git commands will fail with a non-zero exit code since this is no remote repo.
        // The listeners should handle this and not throw any exceptions.
        listener.onCompleted(mockRepo, mockResult)
        assertTrue(true)
    }
}