package com.dangodiary.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * One photo on an [Entry]: the file path plus an optional user-written caption.
 *
 * Caption defaults to empty; an empty caption is rendered as "no caption" in the UI rather
 * than "a caption that says nothing", so the decoder treats the two cases identically.
 */
@Serializable
data class Photo(
    val path: String,
    val caption: String = "",
)

/**
 * Photos on an [Entry] are stored as a JSON-encoded list in [Entry.photoPathsJson].
 *
 * Two JSON shapes are accepted on read for back-compat:
 *  - **Object list** (current): `[{"path":"/a","caption":"x"},...]`
 *  - **String list** (pre-caption): `["/a","/b"]` — each string becomes `Photo(path, "")`
 *
 * On write we always emit the object form. Older rows are silently upgraded the next time
 * the user saves them — no DB migration needed.
 */
object Photos {

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(Photo.serializer())
    private val stringListSerializer = ListSerializer(String.serializer())

    fun encode(photos: List<Photo>): String = json.encodeToString(listSerializer, photos)

    fun decode(encoded: String): List<Photo> {
        if (encoded.isBlank()) return emptyList()
        // Peek at the first non-whitespace structural character to pick the right shape.
        return runCatching {
            val parsed = json.parseToJsonElement(encoded)
            val arr = parsed as? JsonArray ?: return emptyList()
            if (arr.isEmpty()) return emptyList()
            when (arr.first()) {
                is JsonObject -> arr.map {
                    val obj = it.jsonObject
                    Photo(
                        path = obj["path"]?.jsonPrimitive?.content.orEmpty(),
                        caption = obj["caption"]?.jsonPrimitive?.content.orEmpty(),
                    )
                }
                is JsonPrimitive -> arr.map { Photo(path = it.jsonPrimitive.content) }
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    /** Just the paths — handy for callers (PhotoStorage cleanup, list-screen thumbnails). */
    fun paths(encoded: String): List<String> = decode(encoded).map { it.path }
}
