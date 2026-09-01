package com.alethia.model

import com.alethia.config.AlethiaConstants

// Defines the object that will hold all document changes detected by Alethia
data class AlethiaDocument(
    val alethia: AlethiaNote
)

// The wrapper that will hold the metadata of the Alethia note and all
// submitted FlaggedRegions
data class AlethiaNote(
    val version: String = AlethiaConstants.PLUGIN_VERSION,
    val generatedAt: String,
    val flagCount: Int,
    val flaggedRegions: List<FlaggedRegion>
)
