package com.alethia.model

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom GSON Serializer and Deserializer for FlaggedRegion.
 * Registered with GsonBuilder so Gson handles FlaggedRegion
 * conversion automatically - removes manual field mapping from
 * buildNoteJson and readExistingFlags.
 */
class FlaggedRegionAdapter : JsonSerializer<FlaggedRegion>, JsonDeserializer<FlaggedRegion> {

    /**
     * Serializes a FlaggedRegion to a JsonElement.
     * Called automatically by Gson when it encounters a FlaggedRegion.
     */
    override fun serialize(
        src: FlaggedRegion,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonObject().apply {
            addProperty("eventType", src.eventType)
            addProperty("file", src.file)
            addProperty("startLine", src.startLine)
            addProperty("endLine", src.endLine)
            addProperty("charCount", src.charCount)
            addProperty("rationale", src.rationale)
            addProperty("timeStamp", src.timeStamp)
        }
    }

    /**
     * Deserializes a JsonElement back into a FlaggedRegion.
     * Called automatically by Gson when deserializing a JsonElement.
     */
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): FlaggedRegion? {
        if (json == null || !json.isJsonObject) return null
        val obj = json.asJsonObject

        return FlaggedRegion(
            eventType = obj.get("eventType").asString ?: "",
            file = obj.get("file").asString ?: "",
            startLine = obj.get("startLine").asInt,
            endLine = obj.get("endLine").asInt,
            charCount = obj.get("charCount").asInt,
            rationale = obj.get("rationale").asString ?: "",
            timeStamp = obj.get("timeStamp").asString ?: ""
        )
    }
}