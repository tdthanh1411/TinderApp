package com.twilio.conversation.data.model.conversation

import com.twilio.conversation.data.model.localCache.entity.ParticipantDataItem
import java.io.Serializable


data class ConversationListViewItem(
    val sid: String,
    val name: String,
    val participantCount: Int,
    val participantName: String,
    val unreadMessageCount: String,
    val showUnreadMessageCount: Boolean,
    val participatingStatus: Int,
    val lastMessageStateIcon: Int,
    val lastMessageText: String,
    val lastMessageColor: Int,
    val lastMessageDate: String,
    val isMuted: Boolean = false,
    val isLoading: Boolean = false,
    val messageCount: Long,
    val deleted: List<Deleted> = emptyList(),
    val messageIndex: Long,
    val avatarUrl: String,
    val participantItem: List<ParticipantDataItem>
):Serializable
