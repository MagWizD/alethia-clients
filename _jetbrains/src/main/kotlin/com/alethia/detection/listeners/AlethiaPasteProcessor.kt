package com.aletheia.detection.listeners

import com.alethia.detection.AlethiaEventHandler
import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Thin adapter — intercepts paste events and forwards them
 * to AletheiaEventHandler as DetectionEvent objects.
 * Makes no flagging decisions itself.
 * Always returns text unmodified — observing only, never blocking.
 */
class AlethiaPasteProcessor : CopyPastePreProcessor {

    /**
     * Intercepts text before it is copying to clipboard.
     * We are not monitoring copy events, but IntelliJ gets
     * angry if we don't override this function as well - return null
     *
     * @param file              The PSI file being copied from
     * @param startOffsets      Array of start offsets of the copied ranges
     * @param endOffsets        Array of end offsets of the copied ranges
     * @param text              The text being copied
     * @return null             Do not modify copied text
     */
    override fun preprocessOnCopy(
        file: PsiFile?,
        startOffsets: IntArray?,
        endOffsets: IntArray?,
        text: String?
    ): String? {
        return null
    }

    /**
     * Intercepts text before pasting into the editor.
     * Builds DetectionEvent, forwarding it to AletheiaEventHandler.
     * Always returns text unmodified
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
        // If file.path is null, then exit early by returning clipboar text
        val filePath = file.virtualFile?.path ?: return text
        val document = editor.document
        val caretOffset = editor.caretModel.offset
        val startLine = document.getLineNumber(caretOffset) + 1
        val lineCount = text.count { it == '\n' }

        // Sbumit Detection to the AlethiaEventHandler for rules eval
        AlethiaEventHandler.submit(
            DetectionEvent(
                filePath = filePath,
                charCount = text.length,
                startLine = startLine,
                endLine = startLine + lineCount,
                elapsedMs = 0,
                source = EventSource.CLIPBOARD_PASTE
            )
        )

        return text
    }
}