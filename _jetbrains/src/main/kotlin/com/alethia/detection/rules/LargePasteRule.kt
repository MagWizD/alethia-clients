package com.alethia.detection.rules

import com.alethia.config.DetectionConfig
import com.alethia.model.DetectionEvent
import com.alethia.model.EventSource
import com.alethia.model.RuleResult

/**
 * Flagrs large insertions that exceed the configured thresholds.
 * Rationale will vary based on source, paste events are reported
 * with higher confidence than general document changes.
 */
class LargePasteRule(private val config: DetectionConfig) : DetectionRule {

    override fun evaluate(event: DetectionEvent): RuleResult? {
        // Only flag if insertion exceeds the threshold
        if (event.charCount <= config.largePasteThreshold) return null

        // Skip ignored file patterns
        if (config.ignorePatterns.any { event.filePath.contains(it) }) return null

        return when (event.source) {
            EventSource.CLIPBOARD_PASTE -> RuleResult(
                eventType = "LARGE_PASTE",
                rationale = "Large clipboard paste - ${event.charCount} chars pasted from clipboard."
            )
            EventSource.DOCUMENT_CHANGE -> RuleResult(
                eventType = "LARGE_INSERTION",
                rationale = "Large instant insertion - ${event.charCount} chars, source unknown."
            )
        }
    }
}