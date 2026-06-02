package com.alethia.model

/**
 * Represents a single region of code flagged as potentially AI-generated.
 * Instances accumulate during a session and are serialized into git note
 * on commit, then cleared. Schema matches the VSCode extension exactly.
 */
data class FlaggedRegion(
    val file: String,           // Absolute path the file containing the flagged region
    val startLine: Int,         // Line number the flagged region begins (1-indexed)
    val endLine: Int,           // Line number the flagged region ends (1-indexed)
    val charCount: Int,         // Total number of characters in the flagged region
    val rationale: String,      // Human-readable reason for the region being flagged
    val timeStamp: String,      // ISO timestamp of when the flag was created
)
