package com.twilio.conversation.manager

import com.google.gson.Gson
import com.twilio.conversations.Attributes
import com.twilio.conversations.MediaUploadListener
import com.twilio.conversation.common.*
import com.twilio.conversation.common.enums.*
import com.twilio.conversation.common.extensions.*
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.model.localCache.entity.MessageDataItem
import com.twilio.conversation.data.model.message.MessageAttributes
import com.twilio.conversation.repository.ConversationsRepository
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.util.*

interface MessageListManager {
    suspend fun sendTextMessage(conversationSid: String, text: String, uuid: String)
    suspend fun retrySendTextMessage(conversationSid: String, messageUuid: String)
    suspend fun sendMediaMessage(
        conversationSid: String,
        uri: String,
        inputStream: InputStream,
        fileName: String?,
        mimeType: String?,
        messageUuid: String
    )

    suspend fun retrySendMediaMessage(
        conversationSid: String,
        inputStream: InputStream,
        messageUuid: String
    )

    suspend fun updateMessageStatus(messageUuid: String, sendStatus: SendStatus, errorCode: Int = 0)
    suspend fun updateMessageMediaDownloadState(
        conversationSid: String,
        index: Long,
        downloadState: DownloadState,
        downloadedBytes: Long,
        downloadedLocation: String?
    )

    suspend fun setAttributes(
        conversationSid: String,
        index: Long,
        reactions: Reactions,
        typeKey: String,
        identity: String,
        seenStatus: String
    )

    suspend fun notifyMessageRead(conversationSid: String, index: Long)
    suspend fun typing(conversationSid: String)
    suspend fun getMediaContentTemporaryUrl(conversationSid: String, index: Long): String
    suspend fun setMessageMediaDownloadId(conversationSid: String, messageIndex: Long, id: Long)
    suspend fun removeMessage(conversationSid: String, messageIndex: Long)
}

class MessageListManagerImpl(
    private val conversationsClient: ConversationsClient,
    private val conversationsRepository: ConversationsRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : MessageListManager {

    override suspend fun sendTextMessage(conversationSid: String, text: String, uuid: String) {
        val identity = conversationsClient.getConversationsClient().myIdentity
        val conversation =
            conversationsClient.getConversationsClient().getConversation(conversationSid)
        val participantSid = conversation.getParticipantByIdentity(identity).sid
        val attributes = Attributes(uuid)

        val message = MessageDataItem(
            "",
            conversationSid,
            participantSid,
            MessageType.TEXT.value,
            identity,
            Date().time,
            text,
            -1,
            attributes.toString(),
            Direction.OUTGOING.value,
            SendStatus.SENDING.value,
            uuid
        )
        conversationsRepository.insertMessage(message)

        val sentMessage = conversation.sendMessage {
            setAttributes(attributes)
            setBody(text)
        }.toMessageDataItem(identity, uuid)

        conversationsRepository.updateMessageByUuid(sentMessage)
    }

    override suspend fun retrySendTextMessage(conversationSid: String, messageUuid: String) {
        val message =
            withContext(dispatchers.io()) { conversationsRepository.getMessageByUuid(messageUuid) }
                ?: return
        if (message.sendStatus == SendStatus.SENDING.value) return

        conversationsRepository.updateMessageByUuid(message.copy(sendStatus = SendStatus.SENDING.value))

        val identity = conversationsClient.getConversationsClient().myIdentity
        val conversation =
            conversationsClient.getConversationsClient().getConversation(conversationSid)

        val sentMessage = conversation.sendMessage {
            setAttributes(Attributes(message.uuid))
            setBody(message.body)
        }.toMessageDataItem(identity, message.uuid)

        conversationsRepository.updateMessageByUuid(sentMessage)
    }

    override suspend fun sendMediaMessage(
        conversationSid: String,
        uri: String,
        inputStream: InputStream,
        fileName: String?,
        mimeType: String?,
        messageUuid: String
    ) {
        val identity = conversationsClient.getConversationsClient().myIdentity
        val conversation =
            conversationsClient.getConversationsClient().getConversation(conversationSid)
        val participantSid = conversation.getParticipantByIdentity(identity).sid
        val attributes = Attributes(messageUuid)
        val message = MessageDataItem(
            "",
            conversationSid,
            participantSid,
            MessageType.MEDIA.value,
            identity,
            Date().time,
            null,
            -1,
            attributes.toString(),
            Direction.OUTGOING.value,
            SendStatus.SENDING.value,
            messageUuid,
            mediaFileName = fileName,
            mediaUploadUri = uri,
            mediaType = mimeType
        )
        conversationsRepository.insertMessage(message)

        val sentMessage = conversation.sendMessage {
            setAttributes(attributes)
            addMedia(
                inputStream,
                mimeType ?: "",
                fileName,
                createMediaUploadListener(uri, messageUuid)
            )
        }.toMessageDataItem(identity, messageUuid)

        conversationsRepository.updateMessageByUuid(sentMessage)
    }

    override suspend fun retrySendMediaMessage(
        conversationSid: String,
        inputStream: InputStream,
        messageUuid: String
    ) {
        val message =
            withContext(dispatchers.io()) { conversationsRepository.getMessageByUuid(messageUuid) }
                ?: return
        if (message.sendStatus == SendStatus.SENDING.value) return
        if (message.mediaUploadUri == null) {
            return
        }
        conversationsRepository.updateMessageByUuid(message.copy(sendStatus = SendStatus.SENDING.value))
        val identity = conversationsClient.getConversationsClient().myIdentity
        val conversation =
            conversationsClient.getConversationsClient().getConversation(conversationSid)


        val sentMessage = conversation.sendMessage {
            setAttributes(Attributes(messageUuid))
            addMedia(
                inputStream,
                message.mediaType ?: "",
                message.mediaFileName,
                createMediaUploadListener(message.mediaUploadUri, messageUuid)
            )
        }.toMessageDataItem(identity, message.uuid)

        conversationsRepository.updateMessageByUuid(sentMessage)
    }

    private fun createMediaUploadListener(
        uri: String,
        messageUuid: String,
    ): MediaUploadListener {

        return object : MediaUploadListener {
            override fun onStarted() {
                conversationsRepository.updateMessageMediaUploadStatus(
                    messageUuid
                )
            }

            override fun onProgress(uploadedBytes: kotlin.Long) {
                conversationsRepository.updateMessageMediaUploadStatus(
                    messageUuid,
                    uploadedBytes = uploadedBytes
                )
            }

            override fun onCompleted(mediaSid: kotlin.String) {
                conversationsRepository.updateMessageMediaUploadStatus(
                    messageUuid,
                    uploading = false
                )
            }

            override fun onFailed(errorInfo: com.twilio.conversations.ErrorInfo) {
            }
        }

    }

    override suspend fun updateMessageStatus(
        messageUuid: String,
        sendStatus: SendStatus,
        errorCode: Int
    ) {
        conversationsRepository.updateMessageStatus(messageUuid, sendStatus.value, errorCode)
    }

    override suspend fun updateMessageMediaDownloadState(
        conversationSid: String,
        index: Long,
        downloadState: DownloadState,
        downloadedBytes: Long,
        downloadedLocation: String?
    ) {
        val message = conversationsClient.getConversationsClient().getConversation(conversationSid)
            .getMessageByIndex(index)
        conversationsRepository.updateMessageMediaDownloadStatus(
            messageSid = message.sid,
            downloadedBytes = downloadedBytes,
            downloadLocation = downloadedLocation,
            downloadState = downloadState.value
        )
    }

    override suspend fun setAttributes(
        conversationSid: String,
        index: Long,
        reactions: Reactions,
        keyTypeDelete: String,
        identity: String,
        seenStatus: String
    ) {
        val message = conversationsClient
            .getConversationsClient()
            .getConversation(conversationSid)
            .getMessageByIndex(index)

        val reactionsMap: Map<String, Set<String>> =
            reactions.map { it.key.value to it.value }.toMap()

        val typedDeleteMap: Map<String, String> =
            mapOf(keyTypeDelete to identity).toMap()

        val messageAttributes = MessageAttributes(reactionsMap, typedDeleteMap, seenStatus)

        message.setAttributes(Attributes(JSONObject(Gson().toJson(messageAttributes))))
    }

    override suspend fun notifyMessageRead(conversationSid: String, index: Long) {
        val messages = conversationsClient.getConversationsClient().getConversation(conversationSid)
        if (index > messages.lastReadMessageIndex ?: -1) {
            messages.advanceLastReadMessageIndex(index)
        }
    }

    override suspend fun typing(conversationSid: String) {
        conversationsClient.getConversationsClient().getConversation(conversationSid).typing()
    }

    override suspend fun getMediaContentTemporaryUrl(conversationSid: String, index: Long): String {
        val message = conversationsClient.getConversationsClient().getConversation(conversationSid)
            .getMessageByIndex(index)
        return message.firstMedia?.getTemporaryContentUrl()!!
    }

    override suspend fun setMessageMediaDownloadId(
        conversationSid: String,
        messageIndex: Long,
        id: Long
    ) {
        val message = conversationsClient.getConversationsClient().getConversation(conversationSid)
            .getMessageByIndex(messageIndex)
        conversationsRepository.updateMessageMediaDownloadStatus(
            messageSid = message.sid,
            downloadId = id
        )
    }

    override suspend fun removeMessage(conversationSid: String, messageIndex: Long) {
        val message = conversationsClient.getConversationsClient().getConversation(conversationSid)
            .getMessageByIndex(messageIndex)
        conversationsClient.getConversationsClient().getConversation(conversationSid)
            .removeMessage(message)
    }
}
