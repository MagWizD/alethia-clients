package com.alethia.config

/**
 *
 */

data class DetectionConfig(
    val largePasteThreshold: Int = 200,
    val debounceWindowMs: Long = 2000,
    val ignorePatterns: List<String> = listOf(".git")
)