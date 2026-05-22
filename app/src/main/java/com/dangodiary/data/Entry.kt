package com.dangodiary.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    @ColumnInfo(name = "visited_on")
    val visitedOn: Long,

    val rating: Int,

    /** ID from [CuisineCatalog]; null = unset. */
    val cuisine: String? = null,

    /** Legacy single-dish name; superseded by [dishesJson]. Kept on the row for back-compat
     *  and always written as empty by code at or after v3. To be dropped in a future migration. */
    val meal: String = "",

    @ColumnInfo(name = "dishes_json")
    val dishesJson: String = "[]",

    val notes: String = "",

    val companions: String = "",

    /** Legacy single-dish price; superseded by per-dish prices in [dishesJson]. Kept for
     *  back-compat and always written as null by code at or after v3. */
    @ColumnInfo(name = "dish_price_cents")
    val dishPriceCents: Long? = null,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String = "USD",

    @ColumnInfo(name = "address_text")
    val addressText: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,

    @ColumnInfo(name = "photo_paths_json")
    val photoPathsJson: String = "[]",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
