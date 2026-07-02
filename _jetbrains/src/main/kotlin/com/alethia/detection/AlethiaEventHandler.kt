package com.alethia.detection

import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource
import com.alethia.detection.rules.RuleEngine
import com.alethia.model.FlaggedRegion
import com.alethia.session.SessionState

/**
 * Single entry point for all detection events.
 * Receives SessionState via constructor injection, never
 * accesses session state directly. Coupled only to the
 * SessionState interface, not any concrete implementation.
 *
 * Deduplication:
 * CLIPBOARD_PASTE events are recorded per file. Any subsequent
 * DOCUMENT_CHANGE for the same file within PASTE_WINDOW_MS is
 * suppressed, the more specific source wins.
 */
class AletheiaEventHandler(private val sessionState: SessionState) {

    // Tracks recent paste events by file path
    // Used to suppress duplicate DOCUMENT_CHANGE events
    // for the same insertion
    private val recentPastes = mutableMapOf<String, Long>()
    private val PASTE_WINDOW_MS = 500L

    /**
     * Submits a detection event for evaluation.
     * Deduplicates by source priority, evaluates against
     * all registered rules, and queues a flag if warranted.
     *
     * @param event The detection event from any listener
     */
    fun submit(event: DetectionEvent) {

        // Record paste events so any flolowing DOCUMENT_CHANGE
        // events for the same insertion can be suppressed
        if (event.source == EventSource.CLIPBOARD_PASTE) {
            recentPastes[event.filePath] = System.currentTimeMillis()
        }

        // Suppress DOCUMENT_CHANGE if a paste just fired for this file
        // within the deduplication window
        if (event.source == EventSource.DOCUMENT_CHANGE) {
            val lastPaste = recentPastes[event.filePath] ?: 0L
            if (System.currentTimeMillis() - lastPaste < PASTE_WINDOW_MS) return
        }

        // Evaluate against all registered rules
        // null means no rule matched
        val rationale = RuleEngine.evaluate(event) ?: return

        // Build flag and queue via injected session state
        sessionState.addFlag(
            FlaggedRegion(
                file = event.filePath,
                startLine = event.startLine,
                endLine = event.endLine,
                charCount = event.charCount,
                rationale = rationale,
                timeStamp = event.timestamp
            )
        )
    }
}