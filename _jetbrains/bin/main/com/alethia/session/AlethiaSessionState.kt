package com.alethia.session

import com.alethia.model.FlaggedRegion

/**
 * Singleton session state for Alethia plugin.
 * Accumulate FlaggedRegion objects during the session
 * Written to by the AlethiaDocumentListener whe suspicious insertions are detected
 * Read and cleared by the commit handler when commit is made
 *
 * Note: We are using an object rather than a class because it is a
 * singleton by default that can be directly accessed, rather than
 * instantiating the state wherever needed and passing it around.
 */
object AlethiaSessionState {

    // ############  Vars  ############

    // Mutable list of flags queued for the next commit
    private val flaggedRegions = mutableListOf<FlaggedRegion>()

    // ############  Functions  ############

    // Adds a new flag to the queue
    fun addFlag(flag: FlaggedRegion) {
        flaggedRegions.add(flag)
    }

    // Returns a snapshot of the current flage queue
    fun getFlags(): List<FlaggedRegion> {
        return flaggedRegions.toList()
    }

    // Returns the number of currently flagged regions
    fun flagCount(): Int {
        return flaggedRegions.count()
    }

    // Clears all flags - called after flags are written to a git nore for commit
    fun clearFlagd() {
        flaggedRegions.clear()
    }
}