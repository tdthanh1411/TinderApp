package com.twilio.conversation.data.model

data class RepositoryResult<T>(
    val data: T,
    val requestStatus: RepositoryRequestStatus
)