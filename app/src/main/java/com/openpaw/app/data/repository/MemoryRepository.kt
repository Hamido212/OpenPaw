package com.openpaw.app.data.repository

import com.openpaw.app.data.local.MemoryDao
import com.openpaw.app.data.model.Memory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao
) {
    suspend fun remember(key: String, value: String, category: String = "general") {
        val existing = memoryDao.getByKey(key)
        if (existing != null) {
            memoryDao.updateByKey(key, value)
        } else {
            memoryDao.insert(Memory(key = key, value = value, category = category))
        }
    }

    suspend fun recall(key: String): String? = memoryDao.getByKey(key)?.value

    suspend fun getAllMemories(): List<Memory> = memoryDao.getAllMemoriesSync()

    /** Returns a compact string of all memories for inserting into system prompt. */
    suspend fun buildMemoryContext(): String {
        val memories = memoryDao.getAllMemoriesSync()
        if (memories.isEmpty()) return ""
        return buildString {
            append("\n\n[MEMORY]\n")
            memories.forEach { append("- ${it.key}: ${it.value}\n") }
        }
    }

    suspend fun forget(key: String) = memoryDao.deleteByKey(key)
}
