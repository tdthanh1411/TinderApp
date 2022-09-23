package com.twilio.conversation.data.model.conversation

data class ConversationAttributes(
    val avatar: String = "",
    val delete: MutableList<Deleted> = arrayListOf()
)