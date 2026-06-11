package com.alethia.detection.rules

import com.alethia.config.DetectionConfig
import com.alethia.detection.events.DetectionEvent

/**
 * An object that evaluates a DetectionEvent against all the registered rules.
 * Returns the rationale string from the first rule that fires, or null if no
 * rules match.
 * Adding a new rule is simple, just create the rule class and instantiate here
 */
object RuleEngine {
    // Instantiate the config data class
    private val config = DetectionConfig()

    // Register rules here!
    private val rules: List<DetectionRule> = listOf(
        LargePasteRule(config)
        // All new rules will be added here
    )

    // Evaulate the DetectionEvent across our rules suite
    fun evaluate(event: DetectionEvent): String? {
        return rules
            .map { it.evaluate(event) }
            .firstOrNull { it != null }
    }
}