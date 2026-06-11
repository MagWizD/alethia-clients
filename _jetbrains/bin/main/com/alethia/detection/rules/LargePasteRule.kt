package com.alethia.detection.rules

import com.alethia.config.DetectionConfig
import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource

/**
 * Flagrs large insertions that exceed the configured thresholds.
 * Rationale will vary based on source, paste events are reported
 * with higher confidence than general document changes.
 */
class LargePasteRule(private val config: DetectionConfig) : DetectionRule {

    override fun evaluate(event: DetectionEvent): String? {
        // Only flag if insertion exceeds the threshold
        if (event.charCount <= config.largePasteThreshold) return null

        // Skip ignored file patterns
        if (config.ignorePatterns.any { event.filePath.contains(it) }) return null

        return when (event.source) {
            EventSource.CLIPBOARD_PASTE ->
                "Large clipboard paste - ${event.charCount} chars."
            EventSource.DOCUMENT_CHANGE ->
                "Large instance insertion - ${event.charCount} chars in ${event.elapsedMs}ms, source unknown"
        }
    }
}