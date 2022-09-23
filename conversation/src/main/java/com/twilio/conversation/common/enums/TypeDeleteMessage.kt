package com.twilio.conversation.common.enums

enum class TypeDeleteMessage(val type: String) {
    TYPE_DELETE_DEFAULT("DELETE_NORMAL"),
    TYPE_DELETE_ALL("DELETE_ALL"),
    TYPE_DELETE_FOR_YOU("DELETE_FOR_YOU")
}