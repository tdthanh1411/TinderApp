package com.twilio.conversation.data.model.localCache.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.twilio.conversation.data.model.Converters
import com.twilio.conversation.data.model.Participant
import com.twilio.conversation.data.model.ParticipantListViewItem
import javax.annotation.Nullable

@Entity(tableName = "conversation_table")
@TypeConverters(Converters::class)
data class ConversationDataItem(
    @PrimaryKey
    val sid: String,
    val friendlyName: String,
    val attributes: String,
    val uniqueName: String,
    val dateUpdated: Long,
    val dateCreated: Long,
    val lastMessageDate: Long,
    val lastMessageText: String,
    val lastMessageSendStatus: Int,
    val createdBy: String,
    val participantsCount: Long,
    val participantsName: String,
    val messagesCount: Long,
    val unreadMessagesCount: Long,
    val participatingStatus: Int,
    val notificationLevel: Int,
    val messageIndex: Long,
    val participantsList: String

)
