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

    @ColumnInfo(name = "dishes_json")
    val dishesJson: String = "[]",

    val notes: String = "",

    val companions: String = "",

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
