package com.chapp.ui.chat

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Messages")
data class Message(
    @PrimaryKey
    @ColumnInfo(name = "Date")
    val time: Long,
    @ColumnInfo(name = "User")
    val user: String,
    @ColumnInfo(name = "Message")
    val message: String,
    @ColumnInfo(name = "Type")
    val type: Int)