package com.alethia.model

import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Test

class ModelTest {

    @Test
    fun `ProjectEvent round trips through adapter`() {
        val gson = GsonBuilder()
            .registerTypeAdapter(ProjectEvent::class.java, ProjectEventAdapter())
            .create()
        val event = ProjectEvent(
            eventType = "FILE_RENAME",
            rationale = "File renamed",
            timeStamp = "2026-01-01T00:00:00Z",
            details = mutableMapOf("oldPath" to "src/A.kt", "newPath" to "src/B.kt")
        )

        val json = gson.toJson(event)
        val result = gson.fromJson(json, ProjectEvent::class.java)

        assertEquals(event, result)
        assertTrue(json.contains("\"oldPath\""))
    }

    @Test
    fun `ProjectEvent deserializes with missing details`() {
        val gson = GsonBuilder()
            .registerTypeAdapter(ProjectEvent::class.java, ProjectEventAdapter())
            .create()
        val result = gson.fromJson("""{"eventType":"FILE_RENAME"}""", ProjectEvent::class.java)
        assertTrue(result.details.isEmpty())
    }

}