package com.alethia.model


/**
 * Represents the source of a triggered detection event.
 * Priority determines which source wins when multiple events
 * fire for the same insertion, the higher priority takes precedence.
 */
enum class EventType(val priority: Int) {
    // --- Insertion events - can overlap, priority decides which wins ---
    LARGE_INSERTION(0),         // document change, source unconfirmed
    LARGE_PASTE(1),             // confirmed clipboard copy
    FOCUS_RESTORE_PASTE(2),     // paste after IDE regained focus

    // --- Discrete events - no overlap possible -------------------------
    FILE_RENAME(0),
    DIRECTORY_RENAME(0),
    UNUSED_IMPORT(0),
    COMMENT_REMOVED(0),
    COMMIT_MESSAGE_RATIO(0)
}

/**
 * Wraps all the raw data from any detection event into a single object.
 * Listeners build one of these objects then hands it to the EventHandler.
 * This keeps listeners code thin and decoupled from handler.
 */
data class DetectionEvent(
    val filePath: String = "",      // Absolute path of the file where the event occurred
    val repoRoot: String = "",      // Path to the repo root on the local machine
    val charCount: Int = -1,        // Number of chars in teh insertion
    val startLine: Int = -1,        // Line where the insertion begins (1-indexed)
    val endLine: Int = -1,          // Lien where teh insertion ends (1-indexed)
    val elapsedMs: Long = -1L,      // Time since last edit in milliseconds
    val eventType: EventType,       // THe type of event that occurred
    val oldPath: String? = null,    // Original path of the file
    val newPath: String? = null,    // New path of the file
    val timestamp: String = java.time.Instant.now().toString()  // When the event occurred
)