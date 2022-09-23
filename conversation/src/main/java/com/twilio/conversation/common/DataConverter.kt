package com.twilio.conversation.common

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import com.google.gson.Gson
import com.twilio.conversation.common.enums.*
import com.twilio.conversation.common.extensions.*
import com.twilio.conversation.data.model.*
import com.twilio.conversation.data.model.conversation.ConversationAttributes
import com.twilio.conversation.data.model.localCache.entity.ConversationDataItem
import com.twilio.conversation.data.model.conversation.ConversationDetailsViewItem
import com.twilio.conversation.data.model.conversation.ConversationListViewItem
import com.twilio.conversation.data.model.conversation.Deleted
import com.twilio.conversation.data.model.localCache.entity.MessageDataItem
import com.twilio.conversation.data.model.localCache.entity.ParticipantDataItem
import com.twilio.conversation.data.model.message.MessageListViewItem
import com.twilio.conversation.data.model.message.DeleteStatus
import com.twilio.conversation.data.model.message.MessageAttributes
import com.twilio.conversation.manager.friendlyName
import com.twilio.conversations.Conversation
import com.twilio.conversations.Conversation.NotificationLevel
import com.twilio.conversations.Message
import com.twilio.conversations.Participant
import com.twilio.conversations.User

fun Conversation.toConversationDataItem(converters: Converters = Converters()): ConversationDataItem {
    return ConversationDataItem(
        this.sid,
        this.friendlyName,
        this.attributes.toString(),
        this.uniqueName,
        this.dateUpdatedAsDate?.time ?: 0,
        this.dateCreatedAsDate?.time ?: 0,
        0,
        "",
        SendStatus.UNDEFINED.value,
        this.createdBy,
        0,
        "",
        0,
        0,
        this.status.value,
        this.notificationLevel.value,
        0L,
        converters.fromGroupTaskMemberList(asToParticipantsData(this.participantsList))
    )
}

fun ConversationDataItem.asConversationListViewItem(
    context: Context,
    converters: Converters = Converters()
) = ConversationListViewItem(
    this.sid,
    this.friendlyName.ifEmpty { "" },
    this.participantsCount.toInt(),
    this.participantsName,
    this.unreadMessagesCount.asMessageCount(),
    showUnreadMessageCount = this.unreadMessagesCount > 0,
    this.participatingStatus,
    lastMessageStateIcon = SendStatus.fromInt(this.lastMessageSendStatus).asLastMessageStatusIcon(),
    this.lastMessageText,
    lastMessageColor = SendStatus.fromInt(this.lastMessageSendStatus)
        .asLastMessageTextColor(context),
    this.lastMessageDate.asLastMessageDateString(context),
    isMuted = this.notificationLevel == NotificationLevel.MUTED.value,
    messageCount = this.messagesCount,
    deleted = isDelete(attributes),
    messageIndex = this.messageIndex,
    avatarUrl = avatarAttribute(attributes),
    participantItem = converters.toGroupTaskMemberList(this.participantsList)
)

fun asToParticipantsData(participants: List<Participant>): List<ParticipantDataItem> {
    val participantDataItem: MutableList<ParticipantDataItem> = arrayListOf()
    participantDataItem.clear()
    participants.forEach { participant ->
        participantDataItem.add(
            participant.asParticipantDataItem()
        )
    }
    return participantDataItem
}

fun asToParticipantsListItem(participants: List<ParticipantDataItem>): List<ParticipantListViewItem> {
    val participantDataItem: MutableList<ParticipantListViewItem> = arrayListOf()
    participantDataItem.clear()
    participants.forEach { participant ->
        participantDataItem.add(
            participant.toParticipantListViewItem()
        )
    }
    return participantDataItem
}

fun ConversationDataItem.asConversationDetailsViewItem() = ConversationDetailsViewItem(
    this.sid,
    this.friendlyName,
    this.createdBy,
    this.dateCreated.asDateString(),
    this.notificationLevel == NotificationLevel.MUTED.value
)

fun isDelete(attributes: String): List<Deleted> = try {
    Gson().fromJson(attributes, ConversationAttributes::class.java).delete
} catch (e: Exception) {
    emptyList()
}

fun avatarAttribute(attributes: String): String = try {
    Gson().fromJson(attributes, ConversationAttributes::class.java).avatar
} catch (e: Exception) {
    ""
}

fun Message.toMessageDataItem(
    currentUserIdentity: String = participant.identity,
    uuid: String = ""
): MessageDataItem {
    val media = firstMedia  // @todo: support multiple media
    return MessageDataItem(
        this.sid,
        this.conversationSid,
        this.participantSid,
        if (media != null) MessageType.MEDIA.value else MessageType.TEXT.value,
        this.author,
        this.dateCreatedAsDate.time,
        this.body ?: "",
        this.messageIndex,
        this.attributes.toString(),
        if (this.author == currentUserIdentity) Direction.OUTGOING.value else Direction.INCOMING.value,
        if (this.author == currentUserIdentity) SendStatus.SENT.value else SendStatus.UNDEFINED.value,
        uuid,
        media?.sid,
        media?.filename,
        media?.contentType,
        media?.size
    )
}

fun MessageDataItem.toMessageListViewItem(authorChanged: Boolean): MessageListViewItem {
    return MessageListViewItem(
        this.sid,
        this.uuid,
        this.index,
        Direction.fromInt(this.direction),
        this.author,
        authorChanged,
        this.body ?: "",
        this.dateCreated,
        SendStatus.fromInt(sendStatus),
        sendStatusIcon = SendStatus.fromInt(this.sendStatus).asLastMessageStatusIcon(),
        getReactions(attributes).asReactionList(),
        MessageType.fromInt(this.type),
        this.mediaSid,
        this.mediaFileName,
        this.mediaType,
        this.mediaSize,
        this.mediaUri?.toUri(),
        this.mediaDownloadId,
        this.mediaDownloadedBytes,
        DownloadState.fromInt(this.mediaDownloadState),
        this.mediaUploading,
        this.mediaUploadedBytes,
        this.mediaUploadUri?.toUri(),
        this.errorCode,
        this.messagesCount,
        getTypeDelete(attributes).asTypeDelete(),
        getSeenStatus(attributes)
    )
}

fun getReactions(attributes: String): Map<String, Set<String>> = try {
    Gson().fromJson(attributes, MessageAttributes::class.java).reactions
} catch (e: Exception) {
    emptyMap()
}

@SuppressLint("NewApi")
fun Map<String, Set<String>>.asReactionList(): Reactions {
    val reactions: MutableMap<Reaction, Set<String>> = mutableMapOf()
    forEach {
        try {
            reactions[Reaction.fromString(it.key)] = it.value
        } catch (e: Exception) {
        }
    }
    return reactions
}

fun getTypeDelete(attributes: String): Map<String, String> = try {
    Gson().fromJson(attributes, MessageAttributes::class.java).editType
} catch (e: Exception) {
    emptyMap()
}

@SuppressLint("NewApi")
fun Map<String, String>.asTypeDelete(): DeleteStatus {
    var typeDelete = DeleteStatus(TypeDeleteMessage.TYPE_DELETE_DEFAULT.type, "")
    forEach {
        try {
            typeDelete = DeleteStatus(it.key, it.value)

        } catch (e: Exception) {
        }
    }
    return typeDelete
}

fun getSeenStatus(attributes: String): String = try {
    Gson().fromJson(attributes, MessageAttributes::class.java).seenStatus
} catch (e: Exception) {
    ""
}


fun Participant.asParticipantDataItem(typing: Boolean = false, user: User? = null) =
    ParticipantDataItem(
        sid = this.sid,
        conversationSid = this.conversation.sid,
        identity = this.identity,
        friendlyName = user?.friendlyName?.takeIf { it.isNotEmpty() } ?: this.friendlyName
        ?: this.identity,
        isOnline = user?.isOnline ?: false,
        lastReadMessageIndex = this.lastReadMessageIndex,
        lastReadTimestamp = this.lastReadTimestamp,
        typing = typing
    )

fun User.asUserViewItem() = UserViewItem(
    friendlyName = this.friendlyName,
    identity = this.identity
)


fun ParticipantDataItem.toParticipantListViewItem() = ParticipantListViewItem(
    conversationSid = this.conversationSid,
    sid = this.sid,
    identity = this.identity,
    friendlyName = this.friendlyName,
    isOnline = this.isOnline
)

fun List<ConversationDataItem>.asConversationListViewItems(context: Context) =
    map { it.asConversationListViewItem(context) }

fun List<Message>.asMessageDataItems(identity: String) = map { it.toMessageDataItem(identity) }

fun List<MessageDataItem>.asMessageListViewItems() =
    mapIndexed { index, item -> item.toMessageListViewItem(isAuthorChanged(index)) }

private fun List<MessageDataItem>.isAuthorChanged(index: Int): Boolean {
    if (index == 0) return true
    return this[index].author != this[index - 1].author
}

fun List<ParticipantDataItem>.asParticipantListViewItems() = map { it.toParticipantListViewItem() }

fun List<ConversationListViewItem>.merge(oldConversationList: List<ConversationListViewItem>?): List<ConversationListViewItem> {
    val oldConversationMap = oldConversationList?.associate { it.sid to it } ?: return this
    return map { item ->
        val oldItem = oldConversationMap[item.sid] ?: return@map item
        item.copy(isLoading = oldItem.isLoading)
    }
}
