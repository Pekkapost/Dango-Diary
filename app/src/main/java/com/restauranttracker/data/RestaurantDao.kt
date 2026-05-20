package com.restauranttracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {

    @Query("SELECT * FROM restaurants ORDER BY visited_on DESC, created_at DESC")
    fun observeAll(): Flow<List<Restaurant>>

    @Query("SELECT * FROM restaurants WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Restaurant?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(restaurant: Restaurant): Long

    @Update
    suspend fun update(restaurant: Restaurant)

    @Delete
    suspend fun delete(restaurant: Restaurant)
}
