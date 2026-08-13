package com.alethia.detection.events

/**
 * Represents the source of a triggered detection event.
 * Priority determines which source wins when multiple events
 * fire for the same insertion, the higher priority takes precedence.
 */
enum class EventSource(val priority: Int) {
    DOCUMENT_CHANGE(0),     // Large change detected ,source unknown
    CLIPBOARD_PASTE(1),     // Confirmed clipboard paste
}