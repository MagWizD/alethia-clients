package com.alethia.detection.listeners

import com.alethia.test.AlethiaBasePlatformTestCase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

/**
 * Integration tests for AlethiaPasteListener using BasePlatformTestCase.
 * Requires the IntelliJ Platform since the listener hooks into IntelliJ's
 * copy/paste preprocessor extension point.
 *
 * registerVCSRoot() is called per test instead of in setUp() because
 * it triggers an VCS scan that can show a dialog panel, if this happens we can get errors
 * from tests that do not require the VCS root, failing the test due to exceptions.
 *
 * Tests cover paste interception, threshold behavior, and deduplication
 * with AlethiaDocumentListener.
 */
class AlethiaPasteListenerTest : AlethiaBasePlatformTestCase() {

    //  ---------------------------  THRESHOLD TESTS  -------------------------------

    fun `test large paste creates flag`() {
        // VCS root required so getRepoRoot() can resolve the file path
        registerVcsRoot()
        myFixture.openFileInEditor(createRepoFile("test.kt"))
        simulatePaste(myFixture.editor, "x".repeat(1000))
        assertTrue(stateService.getFlags().any { it.eventType == "LARGE_PASTE" })
    }

    fun `test small paste does not create flag`() {
        registerVcsRoot()
        myFixture.openFileInEditor(createRepoFile("test.kt"))
        simulatePaste(myFixture.editor, "x".repeat(50))
        assertEquals(0, stateService.flagCount())
    }

    //  ---------------------------  DEDUPLICATION TESTS  -------------------------------

    fun `test paste suppresses subsequent document change for same file`() {
        myFixture.openFileInEditor(createRepoFile("test.kt"))
        simulatePaste(myFixture.editor, "x".repeat(1000))
        val flagCountAfterPaste = stateService.flagCount()
        myFixture.type("x".repeat(300))
        assertEquals(flagCountAfterPaste, stateService.flagCount())
    }

    //  ---------------------------  HELPER METHOD  -------------------------------

    /**
     * Simulates a real paste in the editor
     */
    private fun simulatePaste(editor: Editor, content: String) {
        // Place contents in clipboard
        CopyPasteManager.getInstance().setContents(StringSelection(content))
        myFixture.performEditorAction("EditorPaste")
    }

}