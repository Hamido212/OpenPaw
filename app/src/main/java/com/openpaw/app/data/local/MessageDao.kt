package com.openpaw.app.data.local

import androidx.room.*
import com.openpaw.app.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionSync(sessionId: String): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Query("SELECT DISTINCT sessionId FROM messages ORDER BY timestamp DESC")
    fun getAllSessionIds(): Flow<List<String>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<Message>

    /**
     * Returns the first USER message for each session, ordered newest session first.
     * Used to show session previews in the chat history list.
     */
    @Query("""
        SELECT m.* FROM messages m
        INNER JOIN (
            SELECT sessionId, MIN(id) AS minId
            FROM messages
            WHERE role = 'USER'
            GROUP BY sessionId
        ) t ON m.id = t.minId
        ORDER BY m.timestamp DESC
    """)
    fun getSessionPreviews(): Flow<List<Message>>

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
