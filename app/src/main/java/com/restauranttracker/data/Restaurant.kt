package com.restauranttracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurants")
data class Restaurant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    @ColumnInfo(name = "visited_on")
    val visitedOn: Long,

    val rating: Int,

    val notes: String = "",

    val companions: String = "",

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
