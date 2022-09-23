package com.twilio.conversation.data.model

data class Participant(
    val sid: String,
    val identity: String,
    val conversationSid: String,
    val friendlyName: String,
    val isOnline: Boolean
)
