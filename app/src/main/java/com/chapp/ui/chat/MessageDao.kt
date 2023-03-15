package com.chapp.ui.chat

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMessage(message: Message)

    @Query("SELECT * FROM Messages ORDER BY Date DESC")
    fun getMessages(): Flow<List<Message>>

    @Query("SELECT * FROM Messages WHERE Date BETWEEN :start AND :end")
    fun getMessages(start: Long?, end: Long?): Flow<List<Message>>

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

}