package com.alethia.model

/**
 * Represents a single region of code flagged as potentially suspicious.
 * Instances accumulate during a session and are serialized into git note
 * on commit, then cleared. Schema matches the VSCode extension exactly.
 */
data class FlaggedRegion(
    val eventType: String,      // Standardized EventID
    val file: String,           // Absolute path the file containing the flagged region
    val startLine: Int,         // Line number the flagged region begins (1-indexed)
    val endLine: Int,           // Line number the flagged region ends (1-indexed)
    val charCount: Int,         // Total number of characters in the flagged region
    val rationale: String,      // Human-readable reason for the region being flagged
    val timeStamp: String,      // ISO timestamp of when the flag was created
)

/**
 * Only used for XML serialization inside AletheiaStateService.
 * Acts as a serializable wrapper class for FlaggedRegion.
 */
class SerializableFlaggedRegion {
    var eventType: String = ""
    var file: String = ""
    var startLine: Int = 0
    var endLine: Int = 0
    var charCount: Int = 0
    var rationale: String = ""
    var timeStamp: String = ""

    /**
     * Convert from FlaggedRegion to serializable form.
     * Create companion object for static use. Now we
     * do not need to instantiate a SerializableFlaggedRegion
     * object to use the from() function. Just reference
     * the class and the function returns an Object made
     * from the passed in FlaggedRegion.
     */
    companion object {
        fun from(flag: FlaggedRegion) = SerializableFlaggedRegion().apply {
            eventType = flag.eventType
            file = flag.file
            startLine = flag.startLine
            endLine = flag.endLine
            charCount = flag.charCount
            rationale = flag.rationale
            timeStamp = flag.timeStamp
        }
    }

    /**
     * Convert SerializableFlaggedRegion back to FlaggedRegion.
     * This is NOT included in the companion object. We require
     * a SerializableFlaggedRegion object in order to load the
     * data.
     */
    fun toFlaggedRegion() = FlaggedRegion(
        eventType = eventType,
        file = file,
        startLine = startLine,
        endLine = endLine,
        charCount = charCount,
        rationale = rationale,
        timeStamp = timeStamp
    )
}