package com.alethia.detection.rules

import com.alethia.model.DetectionEvent
import com.alethia.model.RuleResult

/**
 * An interface that all teh detection rules implement.
 * Each rule evaluates a DetectionEvent and returns a rationale string
 * if the event should be flagged, or null if it should be ignored.
 * Adding a new rule is just implementing this interface and registering
 * it in the RuleEngine.
 */
interface DetectionRule {
    fun evaluate(event: DetectionEvent): RuleResult?
}