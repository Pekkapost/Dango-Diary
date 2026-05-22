package com.dangodiary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries ORDER BY visited_on DESC, created_at DESC")
    fun observeAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Entry?>

    // TRIM + LOWER to match the same normalisation the duplicate-check applies on the client.
    // Most recent visit first so the prompt can offer it as "your previous visit" directly.
    @Query("SELECT * FROM entries WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) ORDER BY visited_on DESC, created_at DESC")
    suspend fun findByNameIgnoreCase(name: String): List<Entry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)
}
