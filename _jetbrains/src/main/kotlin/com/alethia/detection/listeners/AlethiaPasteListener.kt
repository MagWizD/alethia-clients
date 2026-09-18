package com.alethia.detection.listeners

import com.alethia.detection.AlethiaEventHandler
import com.alethia.model.DetectionEvent
import com.alethia.model.EventSource
import com.alethia.utils.getRepoRoot
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.slf4j.LoggerFactory

/**
 * Thin adapter that intercepts paste events and forwards them
 * to AlethiaEventHandler as DetectionEvent objects.
 * Always returns text unmodified.
 */
class AlethiaPasteListener : CopyPastePreProcessor {

    private val LOG = LoggerFactory.getLogger(AlethiaPasteListener::class.java)

    /**
     * Intercepts text before it is copied to the clipboard.
     * We do not monitor copy events — return null to leave
     * the copied text unmodified.
     *
     * @param file          The PSI file being copied from
     * @param startOffsets  Array of start offsets of the copied ranges
     * @param endOffsets    Array of end offsets of the copied ranges
     * @param text          The text being copied
     * @return null         Do not modify copied text
     */
    override fun preprocessOnCopy(
        file: PsiFile?,
        startOffsets: IntArray?,
        endOffsets: IntArray?,
        text: String?
    ): String? = null

    /**
     * Intercepts text before it is pasted into the editor.
     * Builds a DetectionEvent and forwards it to AlethiaEventHandler.
     * Always returns text unmodified — observing only, never blocking.
     *
     * @param project   The currently open project
     * @param file      The PSI file being pasted into
     * @param editor    The active editor where the paste is occurring
     * @param text      The processed clipboard text about to be pasted
     * @param rawText   The raw unprocessed clipboard data — null for plain text
     * @return text unmodified
     */
    override fun preprocessOnPaste(
        project: Project,
        file: PsiFile,
        editor: Editor,
        text: String,
        rawText: RawText?
    ): String {
        LOG.info("alethia paste listener fired.")

        // Get file path — bail out if unavailable, returning text unmodified
        val filePath = file.virtualFile?.path ?: return text

        // Capture caret position now on the EDT before paste lands.
        val document = editor.document
        val startLine = document.getLineNumber(editor.caretModel.offset) + 1
        val lineCount = text.count { it == '\n' }
        val endLine = startLine + lineCount

        // Move repo lookup off the EDT.
        // getRepoRoot() calls GitRepositoryManager.getRepositoryForFile() which
        // is a synchronous VCS operation. IntelliJ does not allow synchronous
        // VCS lookups on the EDT because they can block the UI thread.
        com.intellij.openapi.application.ApplicationManager.getApplication()
            .executeOnPooledThread {
                com.intellij.openapi.application.ApplicationManager.getApplication()
                    .runReadAction {
                        // Retrieve the handler and the repo
                        val repoRoot = getRepoRoot(project, filePath) ?: return@runReadAction
                        val handler = project.service<AlethiaEventHandler>()
                        // Submit the DetectionEvent to AlethiaEventHandler
                        handler.submit(
                            DetectionEvent(
                                filePath = filePath,
                                repoRoot = repoRoot,
                                charCount = text.length,
                                startLine = startLine,
                                endLine = endLine,
                                elapsedMs = 0,
                                source = EventSource.CLIPBOARD_PASTE
                            )
                        )
                    }
            }
        return text
    }
}