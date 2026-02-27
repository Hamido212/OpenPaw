package com.openpaw.app.data.local

import androidx.room.*
import com.openpaw.app.data.model.Memory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    suspend fun getAllMemoriesSync(): List<Memory>

    @Query("SELECT * FROM memories WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): Memory?

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY updatedAt DESC")
    suspend fun getByCategory(category: String): List<Memory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: Memory): Long

    @Query("UPDATE memories SET value = :value, updatedAt = :updatedAt WHERE `key` = :key")
    suspend fun updateByKey(key: String, value: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM memories WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
