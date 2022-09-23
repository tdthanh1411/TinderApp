package com.twilio.conversation.data.model

data class ParticipantListViewItem(
    val sid: String,
    val identity: String,
    val conversationSid: String,
    val friendlyName: String,
    val isOnline: Boolean
) {
    var isSelected = false
}
