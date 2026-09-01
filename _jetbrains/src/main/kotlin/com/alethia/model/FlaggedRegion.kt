package com.alethia.model

/**
 * Represents a single region of code flagged as potentially suspicious.
 * Instances accumulate during a session and are serialized into git note
 * on commit, then cleared. Schema matches the VSCode extension exactly.
 */
data class FlaggedRegion(
    var eventType: String = "",      // Standardized EventID
    var file: String = "",           // Absolute path the file containing the flagged region
    var startLine: Int = 0,         // Line number the flagged region begins (1-indexed)
    var endLine: Int = 0,           // Line number the flagged region ends (1-indexed)
    var charCount: Int = 0,         // Total number of characters in the flagged region
    var rationale: String = "",      // Human-readable reason for the region being flagged
    var timeStamp: String = "",      // ISO timestamp of when the flag was created
)