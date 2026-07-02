package com.alethia.session

import com.alethia.model.FlaggedRegion

/**
 * Interface defining the contract for Alethia session state.
 * Abstracts the underlying persistence mechanism from the rest
 * of the plugin.
 */
interface SessionState {
    fun addFlag(flag: FlaggedRegion)
    fun getFlags(): List<FlaggedRegion>
    fun flagCount(): Int
    fun clearFlags()
    var lastCommitSha: String?
}