package com.alethia.model


/**
 * Represents the source of a triggered detection event.
 * Priority determines which source wins when multiple events
 * fire for the same insertion, the higher priority takes precedence.
 */
enum class EventSource(val priority: Int) {
    DOCUMENT_CHANGE(0),     // Large change detected ,source unknown
    CLIPBOARD_PASTE(1),     // Confirmed clipboard paste
}

/**
 * Wraps all the raw data from any detection event into a single object.
 * Listeners build one of these objects then hands it to the EventHandler.
 * This keeps listeners code thin and decoupled from handler.
 */
data class DetectionEvent(
    val filePath: String,       // Absolute path of the file where the event occurred
    val repoRoot: String,       // Path to the repo root on the local machine
    val charCount: Int,         // Number of chars in teh insertion
    val startLine: Int,         // Line where the insertion begins (1-indexed)
    val endLine: Int,           // Lien where teh insertion ends (1-indexed)
    val elapsedMs: Long,        // Time since last edit in milliseconds
    val source: EventSource,    // Where did this event came from?
    val timestamp: String = java.time.Instant.now().toString()  // When did the event occur?
)