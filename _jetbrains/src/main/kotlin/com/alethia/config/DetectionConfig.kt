package com.alethia.config

/**
 * Data class holding all thresholds set by the repo maintainer.
 * The DetectionConfig is reference by the RuleEngine to evaulate whether
 * the maintainer wants this information requested.
 */
data class DetectionConfig(
    // Minimum number of chars to be considered a large insertion
    val largePasteThreshold: Int = 200,
    // Milliseconds to ignore incoming events, prevents same paste event being caught multiple times
    val debounceWindowMs: Long = 2000,
    // File patterns to ignore
    val ignorePatterns: List<String> = listOf(".git")
)