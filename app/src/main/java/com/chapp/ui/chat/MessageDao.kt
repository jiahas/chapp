package com.chapp.ui.chat

import android.database.Cursor
import androidx.paging.PagingSource
import androidx.room.*

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMessage(message: Message)

    @Query("SELECT * FROM Messages WHERE Date BETWEEN (:start) AND (:end) ORDER BY Date DESC")
    fun getMessages(start: Long?, end: Long?): PagingSource<Int, Message>

    @Query("SELECT * FROM Messages WHERE Date BETWEEN (:start) AND (:end) ORDER BY Date DESC")
    fun exportLog(start: Long?, end: Long?): Cursor

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

}