package com.alethia.detection.listeners

import com.alethia.config.AlethiaConstants
import com.alethia.detection.AlethiaEventHandler
import com.alethia.model.DetectionEvent
import com.alethia.model.EventType
import com.alethia.utils.getRepoRoot
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.slf4j.LoggerFactory


/**
 * Detects large text insertions in open documents.
 * Fires on every document change and forwards to AlethiaEventHandler
 */
class AlethiaDocumentListener(private val project: Project) : DocumentListener {

    private val LOG = LoggerFactory.getLogger(AlethiaDocumentListener::class.java)
    private val handler = project.service<AlethiaEventHandler>()
    private var lastEditTime = System.currentTimeMillis()

    /**
     * Fires after text changes in open docs.
     * Builds DetectionEvent from the raw change data and
     * forwards it to AletheiaEventHandler for rules eval.
     *
     * @param event     The document event containing the change details
     */
    override fun documentChanged(event: DocumentEvent) {

        val now = System.currentTimeMillis()
        val document = event.document

        val filePath = FileDocumentManager.getInstance()
            .getFile(document)?.path ?: return

        // Submit the DetectionEvent to AlethiaEventHandler
        handler.submit(
            DetectionEvent(
                filePath = filePath,
                repoRoot = getRepoRoot(project, filePath) ?: return,
                charCount = event.newFragment.length,
                startLine = document.getLineNumber(event.offset) + 1,
                endLine = document.getLineNumber(event.offset) +
                        event.newFragment.count { it == '\n' } + 1,
                elapsedMs = now - lastEditTime,
                eventType = EventType.LARGE_INSERTION    // Flag this as a general doc change
            )
        )

        lastEditTime = now
    }
}


/**
 * Thin adapter that intercepts paste events and forwards them
 * to AlethiaEventHandler as DetectionEvent objects.
 * Always returns text unmodified.
 */
class AlethiaPasteListener : CopyPastePreProcessor {

    private val LOG = LoggerFactory.getLogger(AlethiaPasteListener::class.java)

    /**
     * Intercepts text before it is copied to the clipboard.
     * We do not monitor copy events - return null to leave
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

        // Get file path
        val filePath = file.virtualFile?.path ?: return text

        // Capture caret position now on the EDT before paste lands.
        val startLine = getStartLine(editor)
        val endLine = startLine + text.count { it == '\n' }
        val eventType = resolveEventType()

        // Submit the event off of the EDT
        submitOffEdt(project, filePath, text, startLine, endLine, eventType)
        return text
    }

    /**
     * Captures the start line from the current caret position.
     * Must run on EDT before the paste lands, after the paste
     * the caret moves to the end of the inserted text.
     */
    private fun getStartLine(editor: Editor): Int =
        editor.document.getLineNumber(editor.caretModel.offset) + 1

    /**
     * Determines whether this paste occurred shortly after the IDE
     * regained focus, a signal that code may have come from an
     * external source.
     */
    private fun resolveEventType(): EventType {
        val timeSinceFocusRestore = System.currentTimeMillis() -
                AlethiaFocusListener.lastFocusRestoreTime
        return if (timeSinceFocusRestore < AlethiaConstants.FOCUS_WINDOW_MS)
            EventType.FOCUS_RESTORE_PASTE
        else
            EventType.LARGE_PASTE
    }

    /**
     * Moves the VCS repo lookup and event submission off the EDT.
     * getRepoRoot() calls GitRepositoryManager.getRepositoryForFile()
     * which is a synchronous VCS operation not allowed on the EDT.
     */
    private fun submitOffEdt(
        project: Project,
        filePath: String,
        text: String,
        startLine: Int,
        endLine: Int,
        eventType: EventType
    ) {
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
                                filePath    = filePath,
                                repoRoot    = repoRoot,
                                charCount   = text.length,
                                startLine   = startLine,
                                endLine     = endLine,
                                elapsedMs   = 0,
                                eventType   = eventType
                            )
                        )
                    }
            }
    }
}