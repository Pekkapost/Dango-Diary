package com.restauranttracker.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Photo paths on a [Restaurant] are stored as a JSON-encoded string of absolute file paths.
 *
 * Using a single column (vs. a separate photos table) keeps the schema flat — we never query
 * across photos and a restaurant rarely has more than a handful. If that changes, split the
 * table out and write a migration; do not redesign in anticipation.
 */
object PhotoPaths {

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(String.serializer())

    fun encode(paths: List<String>): String = json.encodeToString(listSerializer, paths)

    fun decode(encoded: String): List<String> =
        if (encoded.isBlank()) emptyList()
        else runCatching { json.decodeFromString(listSerializer, encoded) }
            .getOrDefault(emptyList())
}
