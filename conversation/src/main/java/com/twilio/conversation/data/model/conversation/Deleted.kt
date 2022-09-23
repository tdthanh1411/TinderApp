package com.twilio.conversation.data.model.conversation

data class Deleted(
    val messageIndex: Long = 0L,
    var status: Boolean = false,
    val identity: String = ""
)
