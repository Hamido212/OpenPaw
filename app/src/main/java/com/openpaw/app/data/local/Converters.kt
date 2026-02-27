package com.openpaw.app.data.local

import androidx.room.TypeConverter
import com.openpaw.app.data.model.MessageRole

class Converters {
    @TypeConverter
    fun fromRole(role: MessageRole): String = role.name

    @TypeConverter
    fun toRole(value: String): MessageRole = MessageRole.valueOf(value)
}
