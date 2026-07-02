package com.alethia.detection.listeners

import com.alethia.detection.AlethiaEventHandler
import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project

/**
 * Thin adapter - Listens to document changes and forwards them
 * to AlethiaEventHandler as DetectionEvent objects.
 */
class AlethiaDocumentListener(private val project: Project) : DocumentListener {

    // Get the handler via the project service
    private val sessionState = project.service<AlethiaSessionState>()
    private val handler = AlethiaEventHandler(sessionState)

    // Record last time a file document change occurred
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
        AlethiaEventHandler.submit(
            DetectionEvent(
                filePath = filePath,
                charCount = event.newFragment.length,
                startLine = document.getLineNumber(event.offset) + 1,
                endLine = document.getLineNumber(event.offset) +
                        event.newFragment.count { it == '\n' } + 1,
                elapsedMs = now - lastEditTime,
                source = EventSource.DOCUMENT_CHANGE    // Flag this as a general doc change
            )
        )

        lastEditTime = now
    }
}