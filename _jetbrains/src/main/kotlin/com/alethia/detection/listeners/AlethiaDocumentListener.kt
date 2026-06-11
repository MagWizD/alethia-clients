package com.alethia.detection.listeners

import com.alethia.detection.AlethiaEventHandler
import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager

/**
 * Thin adapter - Listens to document changes and forwards them
 * to AlethiaEventHandler as DetectionEvent objects.
 */
class AlethiaDocumentListener : DocumentListener {

    // Record last time a file document change occurred
    private var lastEditTime = System.currentTimeMillis()

    // Submit the event to the AlethiaEventHandler for Rule evaluation
    // and flag creation.
    override fun documentChanged(event: DocumentEvent) {
        val now = System.currentTimeMillis()
        val document = event.document

        val filePath = FileDocumentManager.getInstance()
            .getFile(document)?.path ?: return

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