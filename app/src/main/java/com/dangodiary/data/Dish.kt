package com.dangodiary.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * A single dish from a restaurant visit: what was ordered and what it cost (nullable — the
 * user may not record a price).
 *
 * Currency is intentionally NOT per-dish: every dish on one [Entry] shares the entry's
 * [Entry.currencyCode]. A restaurant trip is in one currency.
 */
@Serializable
data class Dish(
    val name: String = "",
    val priceCents: Long? = null,
)

/**
 * Dishes are stored as a JSON-encoded list in [Entry.dishesJson]. Same single-column /
 * single-table choice as [Photos] — we never query across dishes and an entry rarely has
 * more than a handful. Split into a table when that changes; do not redesign in anticipation.
 */
object Dishes {

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(Dish.serializer())

    fun encode(dishes: List<Dish>): String = json.encodeToString(listSerializer, dishes)

    fun decode(encoded: String): List<Dish> =
        if (encoded.isBlank()) emptyList()
        else runCatching { json.decodeFromString(listSerializer, encoded) }
            .getOrDefault(emptyList())
}
