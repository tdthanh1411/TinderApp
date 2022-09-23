package com.twilio.conversation.data.model.localCache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.twilio.conversation.data.model.Converters
import com.twilio.conversation.data.model.localCache.entity.ConversationDataItem
import com.twilio.conversation.data.model.localCache.dao.ConversationsDao
import com.twilio.conversation.data.model.localCache.dao.MessagesDao
import com.twilio.conversation.data.model.localCache.dao.ParticipantsDao
import com.twilio.conversation.data.model.localCache.entity.MessageDataItem
import com.twilio.conversation.data.model.localCache.entity.ParticipantDataItem

@Database(
    entities = [ConversationDataItem::class, MessageDataItem::class, ParticipantDataItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LocalCacheProvider : RoomDatabase() {

    abstract fun conversationsDao(): ConversationsDao

    abstract fun messagesDao(): MessagesDao

    abstract fun participantsDao(): ParticipantsDao

    companion object {
        val INSTANCE get() = _instance ?: error("call LocalCacheProvider.createInstance() first")

        private var _instance: LocalCacheProvider? = null

        fun createInstance(context: Context) {
            check(_instance == null) { "LocalCacheProvider singleton instance has been already created" }
            _instance = Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LocalCacheProvider::class.java
            ).build()
        }
    }
}
