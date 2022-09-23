package com.twilio.conversation.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.twilio.conversation.data.model.localCache.entity.ParticipantDataItem

class Converters {
    @TypeConverter
    fun fromGroupTaskMemberList(value: List<ParticipantDataItem>): String {
        val gson = Gson()
        val type = object : TypeToken<List<ParticipantDataItem>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toGroupTaskMemberList(value: String): List<ParticipantDataItem> {
        val gson = Gson()
        val type = object : TypeToken<List<ParticipantDataItem>>() {}.type
        return gson.fromJson(value, type)
    }
}