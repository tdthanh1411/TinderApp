package com.twilio.conversation.data.model

import com.twilio.conversation.common.enums.ConversationsError

sealed class RepositoryRequestStatus {
    object FETCHING : RepositoryRequestStatus()
    object SUBSCRIBING : RepositoryRequestStatus()
    object COMPLETE : RepositoryRequestStatus()
    class Error(val error: ConversationsError) : RepositoryRequestStatus()
}