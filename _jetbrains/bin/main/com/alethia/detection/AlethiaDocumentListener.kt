package com.alethia.detection

import com.alethia.model.FlaggedRegion
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager

class AlethiaDocumentListener(
    private val config: DetectionConfig = DetectionConfig()
): DocumentListener {

    // Track the lat time any edit was made
    private var lastEditTime = System.currentTimeMillis()

    // Track the last time each file was flagged - this will prevent duplicate
    // flags from a single paste that IntelliJ may chunk into multiple
    private val recentlyFlagged = mutableMapOf<String,Long>()

    // Overwrite the documentChanged() function to execute our rule checks
    // and save the FlaggedRegion if necessary
    override fun documentChanged(event: DocumentEvent) {
        val now = System.currentTimeMillis()
        val charCount = event.newFragment.length
        val document = event.document

        // Get the file path
        val file = FileDocumentManager.getInstance()
            .getFile(document)?.path ?: return

        // Skip git internal files
        if (file.contains(".git")) return

        // Rule1: Large instant insertions
        // Any single change over the set threshold of chars is definitively paste or AI insertion
        if (charCount > config.largePasteThreshold) {
            val lastFlagged = recentlyFlagged[file] ?: 0L

            // Only flag if we haven't flagged this file in the last 2 seconds
            // this will prevent duplicate flags from a single chunked paste event
            if (now - lastFlagged > config.debounceWindowMs) {
                recentlyFlagged[file] = now
                val elapsedMs = now - lastEditTime
                val startLine = document.getLineNumber(event.offset) + 1
                val lineCount = event.newFragment.count { it == '\n' }
                val endLine = startLine + lineCount

                // Create new FlaggedRegion object
                val flag = FlaggedRegion(
                    file = file,
                    startLine = startLine,
                    endLine = endLine,
                    charCount = charCount,
                    rationale = "Large instant insertion - $charCount chars in ${elapsedMs}ms",
                    timeStamp = java.time.Instant.now().toString()
                )

                // Save the FlaggedRegion to the session state
                AlethiaSessionState.addFlag(flag)
            }
        }

        // Update last edit time
        lastEditTime = now
    }
}