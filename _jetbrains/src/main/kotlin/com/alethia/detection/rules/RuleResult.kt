package com.alethia.detection.rules

/**
 * Represents the result of a rule evaluation.
 * Contains a standardized event type for Themis to identify
 * and a rational description.
 */
data class RuleResult(
    val eventType: String,  // Standardized event type
    val rationale: String   // Human-readable description of why this was flagged
)