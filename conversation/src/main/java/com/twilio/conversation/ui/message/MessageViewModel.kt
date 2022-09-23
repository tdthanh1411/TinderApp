package com.twilio.conversation.ui.message

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.twilio.conversation.common.SingleLiveEvent
import com.twilio.conversation.common.avatarAttribute
import com.twilio.conversation.common.enums.*
import com.twilio.conversation.common.extensions.*
import com.twilio.conversation.common.isDelete
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.RepositoryRequestStatus
import com.twilio.conversation.data.model.conversation.Deleted
import com.twilio.conversation.data.model.localCache.entity.ParticipantDataItem
import com.twilio.conversation.data.model.message.MessageListViewItem
import com.twilio.conversation.manager.ConversationListManager
import com.twilio.conversation.manager.MessageListManager
import com.twilio.conversation.repository.ConversationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by ThanhTran on 7/7/2022.
 */

const val MESSAGE_COUNT = 30

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val context: Context,
    private val conversationSid: String,
    private val messageIndex: Long,
    private val conversationsRepository: ConversationsRepository,
    private val messageListManager: MessageListManager,
    private val credentialStorage: CredentialStorage,
    private val conversationListManager: ConversationListManager
) : ViewModel() {

    val selfUser = conversationsRepository.getSelfUser().asLiveData(viewModelScope.coroutineContext)

    val messageItems =
        conversationsRepository.getMessages(conversationSid, MESSAGE_COUNT, messageIndex)
            .onEach { repositoryResult ->
                if (repositoryResult.requestStatus is RepositoryRequestStatus.Error) {
                    onMessageError.postValue(ConversationsError.MESSAGE_FETCH_FAILED)
                }
            }
            .asLiveData(viewModelScope.coroutineContext)
            .map { it.data }

    val onMessageError = SingleLiveEvent<ConversationsError>()

    val onMessageSent = SingleLiveEvent<Unit>()

    val onShowRemoveMessageDialog = SingleLiveEvent<Unit>()

    val onMessageRemoved = SingleLiveEvent<Unit>()

    val onMessageCopied = SingleLiveEvent<Unit>()

    val deletedAttribute = SingleLiveEvent<List<Deleted>>()

    val imageAvatarUrl = SingleLiveEvent<String>()

    var selectedMessageIndex: Long = -1

    val selectedMessage: MessageListViewItem?
        get() = messageItems.value?.firstOrNull {
            it.index == selectedMessageIndex
        }


    val typingParticipantsList =
        conversationsRepository.getTypingParticipants(conversationSid)
            .map { participants -> participants.map { it.typingIndicatorName } }
            .distinctUntilChanged()
            .asLiveData(viewModelScope.coroutineContext)

    private val messagesObserver: Observer<PagedList<MessageListViewItem>> =
        Observer { list ->
            list.forEach { message ->
                if (message?.mediaDownloadState == DownloadState.DOWNLOADING && message.mediaDownloadId != null) {
                    if (updateMessageMediaDownloadState(message.index, message.mediaDownloadId)) {
                        observeMessageMediaDownload(message.index, message.mediaDownloadId)
                    }
                }
            }
        }


    init {
        viewModelScope.launch {
            getConversationResult()
        }
        messageItems.observeForever(messagesObserver)
    }

    override fun onCleared() {
        messageItems.removeObserver(messagesObserver)
    }

    private suspend fun getConversationResult() {
        conversationsRepository.getConversation(conversationSid).collect { result ->
            if (result.requestStatus is RepositoryRequestStatus.Error) {
                onMessageError.value = ConversationsError.CONVERSATION_GET_FAILED
                return@collect
            }
            val attributes = result.data?.attributes.toString()
            deletedAttribute.value = isDelete(attributes)
            imageAvatarUrl.value = avatarAttribute(attributes)

        }
    }

    fun sendTextMessage(message: String) = viewModelScope.launch {
        val messageUuid = UUID.randomUUID().toString()
        try {
            messageListManager.sendTextMessage(conversationSid, message, messageUuid)
            onMessageSent.call()
        } catch (e: ConversationsException) {
            messageListManager.updateMessageStatus(
                messageUuid,
                SendStatus.ERROR,
                e.errorInfo?.code ?: 0
            )
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
        }
    }

    fun resendTextMessage(messageUuid: String) = viewModelScope.launch {
        try {
            messageListManager.retrySendTextMessage(conversationSid, messageUuid)
            onMessageSent.call()
        } catch (e: ConversationsException) {
            messageListManager.updateMessageStatus(
                messageUuid,
                SendStatus.ERROR,
                e.errorInfo?.code ?: 0
            )
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
        }
    }

    fun sendMediaMessage(
        uri: String,
        inputStream: InputStream,
        fileName: String?,
        mimeType: String?
    ) =
        viewModelScope.launch {
            val messageUuid = UUID.randomUUID().toString()
            try {
                messageListManager.sendMediaMessage(
                    conversationSid,
                    uri,
                    inputStream,
                    fileName,
                    mimeType,
                    messageUuid
                )
                onMessageSent.call()
            } catch (e: ConversationsException) {
                messageListManager.updateMessageStatus(
                    messageUuid,
                    SendStatus.ERROR,
                    e.errorInfo?.code ?: 0
                )
                onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            }
        }

    fun resendMediaMessage(inputStream: InputStream, messageUuid: String) = viewModelScope.launch {
        try {
            messageListManager.retrySendMediaMessage(conversationSid, inputStream, messageUuid)
            onMessageSent.call()
        } catch (e: ConversationsException) {
            messageListManager.updateMessageStatus(
                messageUuid,
                SendStatus.ERROR,
                e.errorInfo?.code ?: 0
            )
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
        }
    }

    fun handleMessageDisplayed(messageIndex: Long) = viewModelScope.launch {
        try {
            messageListManager.notifyMessageRead(conversationSid, messageIndex)
        } catch (e: ConversationsException) {
            // Ignored
        }
    }

    fun typing() = viewModelScope.launch {
        messageListManager.typing(conversationSid)
    }

    fun setAttributes(reactions: Reactions, typeKeyDelete: String, seenStatus: String) =
        viewModelScope.launch {
            try {
                messageListManager.setAttributes(
                    conversationSid,
                    selectedMessageIndex,
                    reactions,
                    typeKeyDelete,
                    credentialStorage.identity,
                    seenStatus
                )
            } catch (e: ConversationsException) {
                onMessageError.value = ConversationsError.REACTION_UPDATE_FAILED
            }
        }

    fun setAttributeConversations(
        conversationSid: String,
        deleted: List<Deleted>,
        messageIndex: Long,
        avatarUrl:String
    ) =
        viewModelScope.launch {
            try {
                conversationListManager.setAttributes(
                    conversationSid = conversationSid,
                    idSend = true,
                    identity = credentialStorage.identity,
                    deleted = deleted,
                    messageIndex = messageIndex,
                    false,
                    avatarUrl
                )
            } catch (e: ConversationsException) {
                Log.e("THANH1234567", "setAttributeConversations: ${e.errorInfo}")
            }
        }


    fun copyMessageToClipboard() {
        try {
            val message = selectedMessage ?: error("No message selected")
            val clip = ClipData.newPlainText("Message text", message.body)
            val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
            clipboard?.setPrimaryClip(clip)
            onMessageCopied.call()
        } catch (e: Exception) {
            onMessageError.value = ConversationsError.MESSAGE_COPY_FAILED
        }
    }

    fun removeMessage() = viewModelScope.launch {
        try {
            messageListManager.removeMessage(conversationSid, selectedMessageIndex)
            onMessageRemoved.call()
        } catch (e: ConversationsException) {
            onMessageError.value = ConversationsError.MESSAGE_REMOVE_FAILED
        }
    }

    fun updateMessageMediaDownloadStatus(
        messageIndex: Long,
        downloadState: DownloadState,
        downloadedBytes: Long = 0,
        downloadedLocation: String? = null
    ) = viewModelScope.launch {
        messageListManager.updateMessageMediaDownloadState(
            conversationSid,
            messageIndex,
            downloadState,
            downloadedBytes,
            downloadedLocation
        )
    }

    fun startMessageMediaDownload(messageIndex: Long, fileName: String?) = viewModelScope.launch {
        updateMessageMediaDownloadStatus(messageIndex, DownloadState.DOWNLOADING)

        val sourceUriResult =
            runCatching {
                Uri.parse(
                    messageListManager.getMediaContentTemporaryUrl(
                        conversationSid,
                        messageIndex
                    )
                )
            }
        val sourceUri = sourceUriResult.getOrElse { e ->
            updateMessageMediaDownloadStatus(messageIndex, DownloadState.ERROR)
            return@launch
        }

        val downloadManager =
            context.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val downloadRequest = DownloadManager.Request(sourceUri).apply {
            setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileName ?: sourceUri.pathSegments.last()
            )
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }
        val downloadId = downloadManager.enqueue(downloadRequest)

        messageListManager.setMessageMediaDownloadId(conversationSid, messageIndex, downloadId)
        observeMessageMediaDownload(messageIndex, downloadId)
    }

    private fun observeMessageMediaDownload(messageIndex: Long, downloadId: Long) {
        val downloadManager =
            context.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val downloadCursor = downloadManager.queryById(downloadId)
        val downloadObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (!updateMessageMediaDownloadState(messageIndex, downloadId)) {
                    downloadCursor.unregisterContentObserver(this)
                    downloadCursor.close()
                }
            }
        }
        downloadCursor.registerContentObserver(downloadObserver)
    }

    /**
     * Notifies the view model of the current download state
     * @return true if the download is still in progress
     */
    private fun updateMessageMediaDownloadState(messageIndex: Long, downloadId: Long): Boolean {
        val downloadManager =
            context.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.queryById(downloadId)

        if (!cursor.moveToFirst()) {
            cursor.close()
            return false
        }

        val status = cursor.getInt(DownloadManager.COLUMN_STATUS)
        val downloadInProgress =
            status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL
        val downloadedBytes = cursor.getLong(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

        updateMessageMediaDownloadStatus(messageIndex, DownloadState.DOWNLOADING, downloadedBytes)

        when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                val downloadedFile =
                    cursor.getString(DownloadManager.COLUMN_LOCAL_URI).toUri().toFile()
                val downloadedLocation =
                    FileProvider.getUriForFile(
                        context,
                        "com.twilio.conversation.fileprovider",
                        downloadedFile
                    )
                        .toString()
                updateMessageMediaDownloadStatus(
                    messageIndex,
                    DownloadState.COMPLETED,
                    downloadedBytes,
                    downloadedLocation
                )
            }
            DownloadManager.STATUS_FAILED -> {
                onMessageError.value = ConversationsError.MESSAGE_MEDIA_DOWNLOAD_FAILED
                updateMessageMediaDownloadStatus(messageIndex, DownloadState.ERROR, downloadedBytes)
            }
        }

        cursor.close()
        return downloadInProgress
    }

    private val ParticipantDataItem.typingIndicatorName get() = friendlyName.ifEmpty { identity }


    @Suppress("UNCHECKED_CAST")
    class MessageViewModelFactory @Inject constructor(
        private val context: Context,
        private val conversationSid: String,
        private val messageIndex: Long,
        private val conversationsRepository: ConversationsRepository,
        private val messageListManager: MessageListManager,
        private val credentialStorage: CredentialStorage,
        private val conversationListManager: ConversationListManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
                return MessageViewModel(
                    context,
                    conversationSid,
                    messageIndex,
                    conversationsRepository,
                    messageListManager,
                    credentialStorage,
                    conversationListManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }

    }


}