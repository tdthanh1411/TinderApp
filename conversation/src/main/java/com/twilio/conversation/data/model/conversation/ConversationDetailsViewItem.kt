package com.twilio.conversation.data.model.conversation

data class ConversationDetailsViewItem(
    val conversationSid: String,
    val conversationName: String,
    val createdBy: String,
    val dateCreated: String,
    val isMuted: Boolean = false
)
