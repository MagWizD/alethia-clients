package com.alethia.model

/**
 * Only used for XML serialization inside AletheiaStateService.
 * Acts as a serializable wrapper class for FlaggedRegion.
 */
class SerializableFlaggedRegion {
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
        file = file,
        startLine = startLine,
        endLine = endLine,
        charCount = charCount,
        rationale = rationale,
        timeStamp = timeStamp
    )
}