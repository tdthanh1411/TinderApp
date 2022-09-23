package com.twilio.conversation.data.model.message

data class MessageAttributes(
    val reactions: Map<String, Set<String>> = mapOf(),
    val editType: Map<String, String> = mapOf(),
    val seenStatus: String = ""
)
