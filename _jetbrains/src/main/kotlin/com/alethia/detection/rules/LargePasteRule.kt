package com.alethia.detection.rules

import com.alethia.config.DetectionConfig
import com.alethia.model.DetectionEvent
import com.alethia.model.EventType

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

        return when (event.eventType) {
            EventType.LARGE_PASTE -> RuleResult(
                eventType = "LARGE_PASTE",
                rationale = "Large clipboard paste - ${event.charCount} chars pasted from clipboard."
            )
            EventType.LARGE_INSERTION -> RuleResult(
                eventType = "LARGE_INSERTION",
                rationale = "Large instant insertion - ${event.charCount} chars, source unknown."
            )
            EventType.FOCUS_RESTORE_PASTE -> RuleResult(
                eventType = "FOCUS_RESTORE_PASTE",
                rationale = "Large clipboard paste within re-focus window - ${event.charCount} chars pasted from clipboard."
            )

            else -> {
                // Return null, if no matches, then no results should be sent.
                null
            }
        }
    }
}