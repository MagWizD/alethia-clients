package com.alethia.detection.listeners

import com.alethia.detection.AlethiaEventHandler
import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource
import com.alethia.session.AlethiaStateService
import com.alethia.utils.getRepoRoot
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Thin adapter that intercepts paste events and forwards them
 * to AlethiaEventHandler as DetectionEvent objects.
 * Always returns text unmodified.
 */
class AlethiaPasteProcessor : CopyPastePreProcessor {
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
        // Get file path — bail out if unavailable, returning text unmodified
        val filePath = file.virtualFile?.path ?: return text

        // Get repo root — bail out if file is not inside a git repo
        val repoRoot = getRepoRoot(project, filePath) ?: return text

        val document = editor.document
        val caretOffset = editor.caretModel.offset
        val startLine = document.getLineNumber(caretOffset) + 1
        val lineCount = text.count { it == '\n' }

        // Build handler from project service and submit detection event
        // TODO: move to constructor injection once plugin.xml supports
        // project-scoped paste processor registration
        val handler = AlethiaEventHandler(project.service<AlethiaStateService>())

        handler.submit(
            DetectionEvent(
                filePath = filePath,
                repoRoot = repoRoot,
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