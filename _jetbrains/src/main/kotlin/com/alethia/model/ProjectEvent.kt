package com.alethia.model

/**
 * Represents a single event within the project flagged as potentially suspicious.
 * Instances accumulate during a session and are serialized into git note
 * on commit, then clear.
 */
data class ProjectEvent (
    var eventType: String = "",
    var rationale: String = "",
    var timeStamp: String = "",
    val details: Map<String, String> = emptyMap()
)