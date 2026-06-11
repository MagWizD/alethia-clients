package com.alethia.detection

import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource
import com.alethia.detection.rules.RuleEngine
import com.alethia.model.FlaggedRegion
import com.alethia.session.AlethiaSessionState

/**
 * Object that acts as the single entry point for all detection events.
 * Receives events from all listeners, deduplicates them by source heirarchy,
 * delegates rule evaluation to RuleEngine, and queues flags in the session state.
 *
 * Deduplication logic:
 * If a CLIPBOARD_PASTE event arrives for a file, any following DOCUMENT_CHANGE
 * event will be dropped - the more specific source wins. A short window gives
 * all listeners time to fire before decisions are made.
 */
object AlethiaEventHandler {

    // Tracks recent paste events by file
    // This is used to suppress duplicate events from same insertion
    private val recentPastes = mutableMapOf<String, Long>()
    private const val PASTE_WINDOW_MS = 500L

    // Submits the event to RuleEngine for evaluation and submits
    // flag to Session State
    fun submit(event: DetectionEvent) {

        // If this is a paste, then record is so following DOCUMENT_CHANGE
        // events are ignored.
        if (event.source == EventSource.CLIPBOARD_PASTE) {
            recentPastes[event.filePath] = System.currentTimeMillis()
        }

        // If this is a document change , check if a paste just fire for this file.
        if (event.source == EventSource.DOCUMENT_CHANGE) {
            val lastPaste = recentPastes[event.filePath] ?: 0L
            if (System.currentTimeMillis() - lastPaste > PASTE_WINDOW_MS) return
        }

        // Submit event to RuleEngine to evaluate against all registed rules
        val rationale = RuleEngine.evaluate(event) ?: return

        // Build and queue flag
        AlethiaSessionState.addFlag(
            FlaggedRegion(
                file = event.filePath,
                startLine = event.startLine,
                endLine = event.endLine,
                charCount = event.charCount,
                rationale = rationale,
                timeStamp = event.timestamp,
            )
        )
    }
}