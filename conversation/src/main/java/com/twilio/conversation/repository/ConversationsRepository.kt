package com.twilio.conversation.repository

import android.util.Log
import androidx.core.view.isVisible
import androidx.lifecycle.Transformations.map
import androidx.paging.PagedList
import com.twilio.conversation.common.*
import com.twilio.conversation.common.enums.CrashIn
import com.twilio.conversation.common.enums.TypeDeleteMessage
import com.twilio.conversation.common.extensions.*
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.Converters
import com.twilio.conversation.data.model.localCache.entity.ConversationDataItem
import com.twilio.conversation.data.model.RepositoryRequestStatus
import com.twilio.conversation.data.model.RepositoryRequestStatus.*
import com.twilio.conversation.data.model.RepositoryResult
import com.twilio.conversation.data.model.localCache.LocalCacheProvider
import com.twilio.conversation.data.model.localCache.entity.MessageDataItem
import com.twilio.conversation.data.model.localCache.entity.ParticipantDataItem
import com.twilio.conversation.data.model.message.MessageListViewItem
import com.twilio.conversations.*
import com.twilio.conversations.Participant.Type.CHAT
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

interface ConversationsRepository {
    fun getUserConversations(): Flow<RepositoryResult<List<ConversationDataItem>>>
    fun getConversation(conversationSid: String): Flow<RepositoryResult<ConversationDataItem?>>
    fun getSelfUser(): Flow<User>
    fun getMessageByUuid(messageUuid: String): MessageDataItem?

    fun getMessages(
        conversationSid: String,
        pageSize: Int,
        messageIndex: Long
    ): Flow<RepositoryResult<PagedList<MessageListViewItem>>>

    fun insertMessage(message: MessageDataItem)
    fun updateMessageByUuid(message: MessageDataItem)
    fun updateMessageStatus(messageUuid: String, sendStatus: Int, errorCode: Int)
    fun getTypingParticipants(conversationSid: String): Flow<List<ParticipantDataItem>>
    fun getConversationParticipants(conversationSid: String): Flow<RepositoryResult<List<ParticipantDataItem>>>
    fun updateMessageMediaDownloadStatus(
        messageSid: String,
        downloadId: Long? = null,
        downloadLocation: String? = null,
        downloadState: Int? = null,
        downloadedBytes: Long? = null
    )

    fun updateMessageMediaUploadStatus(
        messageUuid: String,
        uploading: Boolean? = null,
        uploadedBytes: Long? = null
    )

    fun simulateCrash(where: CrashIn)
    fun clear()
    fun subscribeToConversationsClientEvents()
    fun unsubscribeFromConversationsClientEvents()
}

class ConversationsRepositoryImpl(
    private val conversationsClientWrapper: ConversationsClient,
    private val localCache: LocalCacheProvider,
    private val credentialStorage: CredentialStorage,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
) : ConversationsRepository {

    private val repositoryScope = CoroutineScope(dispatchers.io() + SupervisorJob())

    private val clientListener = createClientListener(
        onConversationDeleted = { conversation ->
            launch {
                localCache.conversationsDao().delete(conversation.sid)
            }
        },
        onConversationUpdated = { conversation, _ ->
            launch {
                insertOrUpdateConversation(conversation.sid)
            }
        },
        onConversationAdded = { conversation ->
            launch {
                insertOrUpdateConversation(conversation.sid)
            }
        },
        onConversationSynchronizationChange = { conversation ->
            launch { insertOrUpdateConversation(conversation.sid) }
        }
    )

    private val conversationListener = createConversationListener(
        onTypingStarted = { conversation, participant ->
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao()
                    .insertOrReplace(participant.asParticipantDataItem(typing = true, user))
            }
        },
        onTypingEnded = { conversation, participant ->
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao()
                    .insertOrReplace(participant.asParticipantDataItem(typing = false, user))
            }
        },
        onParticipantAdded = { participant ->
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao()
                    .insertOrReplace(participant.asParticipantDataItem(user = user))
            }
        },
        onParticipantUpdated = { participant, reason ->
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao()
                    .insertOrReplace(participant.asParticipantDataItem(user = user))
            }
        },
        onParticipantDeleted = { participant ->
            this@ConversationsRepositoryImpl.launch {
                localCache.participantsDao().delete(participant.asParticipantDataItem())
            }
        },
        onMessageDeleted = { message ->
            deleteMessage(message)
        },
        onMessageUpdated = { message, reason ->
            updateMessage(message, reason)
        },
        onMessageAdded = { message ->
            addMessage(message)
        }
    )

    private fun launch(block: suspend CoroutineScope.() -> Unit) = repositoryScope.launch(
        context = CoroutineExceptionHandler { _, e ->
            Log.e("launch", "Coroutine failed ${e.localizedMessage}")
        },
        block = block
    )

    override fun getUserConversations(): Flow<RepositoryResult<List<ConversationDataItem>>> {
        val localDataFlow = localCache.conversationsDao().getUserConversations()
        val fetchStatusFlow = fetchConversations().flowOn(dispatchers.io())

        return combine(localDataFlow, fetchStatusFlow) { data, status ->
            RepositoryResult(
                data,
                status
            )
        }
    }

    override fun getConversation(conversationSid: String): Flow<RepositoryResult<ConversationDataItem?>> {
        val localDataFlow = localCache.conversationsDao().getConversation(conversationSid)
        val fetchStatusFlow = fetchConversation(conversationSid).flowOn(dispatchers.io())

        return combine(localDataFlow, fetchStatusFlow) { data, status ->
            RepositoryResult(
                data,
                status
            )
        }
    }

    override fun getMessageByUuid(messageUuid: String) =
        localCache.messagesDao().getMessageByUuid(messageUuid)

    @OptIn(ObsoleteCoroutinesApi::class)
    override fun getMessages(
        conversationSid: String,
        pageSize: Int,
        messageIndex: Long
    ): Flow<RepositoryResult<PagedList<MessageListViewItem>>> {
        val requestStatusConversation = BroadcastChannel<RepositoryRequestStatus>(BUFFERED)
        val boundaryCallback = object : PagedList.BoundaryCallback<MessageListViewItem>() {
            override fun onZeroItemsLoaded() {
                launch {
                    fetchMessages(conversationSid) { getLastMessages(pageSize) }
                        .flowOn(dispatchers.io())
                        .collect {
                            requestStatusConversation.send(it)
                        }
                }
            }

            override fun onItemAtEndLoaded(itemAtEnd: MessageListViewItem) {
            }

            override fun onItemAtFrontLoaded(itemAtFront: MessageListViewItem) {
                if (itemAtFront.index > messageIndex) {
                    launch {

                        fetchMessages(conversationSid) {
                            getMessagesBefore(
                                itemAtFront.index - 1,
                                pageSize
                            )
                        }
                            .flowOn(dispatchers.io())
                            .collect {
                                requestStatusConversation.send(it)
                            }
                    }
                }
            }
        }

        val pagedListFlow = if (messageIndex == 0L) {
            localCache.messagesDao().getMessagesSorted(conversationSid)
                .mapByPage {
                    it?.asMessageListViewItems()
                }
                .toFlow(
                    pageSize = pageSize,
                    boundaryCallback = boundaryCallback
                )
                .onStart {
                    requestStatusConversation.send(FETCHING)
                }
                .onEach {
                    requestStatusConversation.send(COMPLETE)
                }
        } else {
            localCache.messagesDao().getMessagesFromIndex(conversationSid, messageIndex)
                .mapByPage {
                    it?.asMessageListViewItems()
                }
                .toFlow(
                    pageSize = pageSize,
                    boundaryCallback = boundaryCallback
                )
                .onStart {
                    requestStatusConversation.send(FETCHING)
                }
                .onEach {
                    requestStatusConversation.send(COMPLETE)
                }
        }

        return combine(
            pagedListFlow,
            requestStatusConversation.asFlow().distinctUntilChanged()
        ) { data, status ->
            RepositoryResult(data, status)
        }
    }

    override fun insertMessage(message: MessageDataItem) {
        launch {
            localCache.messagesDao().insertOrReplace(message)
            updateConversationLastMessage(message.conversationSid)
        }
    }

    override fun updateMessageByUuid(message: MessageDataItem) {
        launch {
            localCache.messagesDao().updateByUuidOrInsert(message)
            updateConversationLastMessage(message.conversationSid)
        }
    }

    override fun updateMessageStatus(messageUuid: String, sendStatus: Int, errorCode: Int) {
        launch {
            localCache.messagesDao().updateMessageStatus(messageUuid, sendStatus, errorCode)

            val message = localCache.messagesDao().getMessageByUuid(messageUuid) ?: return@launch
            updateConversationLastMessage(message.conversationSid)
        }
    }

    override fun getTypingParticipants(conversationSid: String): Flow<List<ParticipantDataItem>> =
        localCache.participantsDao().getTypingParticipants(conversationSid)

    override fun getConversationParticipants(conversationSid: String): Flow<RepositoryResult<List<ParticipantDataItem>>> {
        val localDataFlow = localCache.participantsDao().getAllParticipants(conversationSid)
        val fetchStatusFlow = fetchParticipants(conversationSid).flowOn(dispatchers.io())

        return combine(localDataFlow, fetchStatusFlow) { data, status ->
            RepositoryResult(
                data,
                status
            )
        }
    }

    override fun updateMessageMediaDownloadStatus(
        messageSid: String,
        downloadId: Long?,
        downloadLocation: String?,
        downloadState: Int?,
        downloadedBytes: Long?
    ) {
        launch {
            if (downloadId != null) {
                localCache.messagesDao().updateMediaDownloadId(messageSid, downloadId)
            }
            if (downloadLocation != null) {
                localCache.messagesDao().updateMediaDownloadLocation(messageSid, downloadLocation)
            }
            if (downloadState != null) {
                localCache.messagesDao().updateMediaDownloadState(messageSid, downloadState)
            }
            if (downloadedBytes != null) {
                localCache.messagesDao().updateMediaDownloadedBytes(messageSid, downloadedBytes)
            }
        }
    }

    override fun updateMessageMediaUploadStatus(
        messageUuid: String,
        uploading: Boolean?,
        uploadedBytes: Long?
    ) {
        launch {
            if (uploading != null) {
                localCache.messagesDao().updateMediaUploadStatus(messageUuid, uploading)
            }
            if (uploadedBytes != null) {
                localCache.messagesDao().updateMediaUploadedBytes(messageUuid, uploadedBytes)
            }
        }
    }

    override fun simulateCrash(where: CrashIn) {
        launch {
            conversationsClientWrapper.getConversationsClient().simulateCrash(where)
        }
    }

    override fun clear() {
        launch {
            localCache.clearAllTables()
        }
    }

    override fun getSelfUser(): Flow<User> = callbackFlow {
        val client = conversationsClientWrapper.getConversationsClient()
        val listener = createClientListener(
            onUserUpdated = { user, _ ->
                user.takeIf { it.identity == client.myIdentity }
                    ?.let { trySend(it).isSuccess }
            }
        )
        client.addListener(listener)
        send(client.myUser)
        awaitClose { client.removeListener(listener) }
    }

    private fun fetchMessages(
        conversationSid: String,
        fetch: suspend Conversation.() -> List<Message>
    ) = flow {
        emit(FETCHING)
        try {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            val messages = conversationsClientWrapper
                .getConversationsClient()
                .getConversation(conversationSid)
                .waitForSynchronization()
                .fetch()
                .asMessageDataItems(identity)
            localCache.messagesDao().insert(messages)
            if (messages.isNotEmpty()) {
                updateConversationLastMessage(conversationSid)
            }
            emit(COMPLETE)
        } catch (e: com.twilio.conversation.common.extensions.ConversationsException) {
            emit(Error(e.error))
        }
    }

    private fun fetchConversation(conversationSid: String) = flow {
        emit(FETCHING)
        try {
            insertOrUpdateConversation(conversationSid)
            emit(COMPLETE)
        } catch (e: com.twilio.conversation.common.extensions.ConversationsException) {
            emit(Error(e.error))
        }
    }

    private fun fetchParticipants(conversationSid: String) = flow {
        emit(FETCHING)
        try {
            val conversation =
                conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
            conversation.waitForSynchronization()
            conversation.participantsList.forEach { participant ->
                // Getting user is currently supported for chat participants only
                val user = if (participant.type == CHAT) participant.getAndSubscribeUser() else null
                localCache.participantsDao()
                    .insertOrReplace(participant.asParticipantDataItem(user = user))
            }
            emit(COMPLETE)
        } catch (e: com.twilio.conversation.common.extensions.ConversationsException) {
            emit(Error(e.error))
        }
    }

    private fun fetchConversations() = channelFlow {
        send(FETCHING)

        try {
            // get items from client
            val dataItems = conversationsClientWrapper
                .getConversationsClient()
                .myConversations
                .map { it.toConversationDataItem() }

            localCache.conversationsDao().deleteGoneUserConversations(dataItems)
            send(SUBSCRIBING)

            var status: RepositoryRequestStatus = COMPLETE
            supervisorScope {
                // get all conversations and update conversation data in local cache
                dataItems.forEach {
                    launch {
                        try {
                            insertOrUpdateConversation(it.sid)
                        } catch (e: com.twilio.conversation.common.extensions.ConversationsException) {
                            status = Error(e.error)
                        }
                    }
                }
            }
            send(status)
        } catch (e: com.twilio.conversation.common.extensions.ConversationsException) {
            send(Error(e.error))
        }
    }

    override fun subscribeToConversationsClientEvents() {
        launch {
            conversationsClientWrapper.getConversationsClient().addListener(clientListener)
        }
    }

    override fun unsubscribeFromConversationsClientEvents() {
        launch {
            conversationsClientWrapper.getConversationsClient().removeListener(clientListener)
        }
    }

    private suspend fun insertOrUpdateConversation(conversationSid: String,converters: Converters = Converters()) {
        val conversation =
            conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
        conversation.addListener(conversationListener)
        localCache.conversationsDao().insert(conversation.toConversationDataItem())
        localCache.conversationsDao().update(
            conversation.sid,
            conversation.status.value,
            conversation.notificationLevel.value,
            conversation.friendlyName
        )
        launch {
            localCache.conversationsDao()
                .updateParticipantCount(conversationSid, conversation.getParticipantCount())
        }
        launch {
            localCache.conversationsDao().updateParticipantName(
                conversationSid,
                conversation.getParticipantName(credentialStorage)
            )
        }
        launch {
            localCache.conversationsDao().updateParticipantList(conversationSid,converters.fromGroupTaskMemberList(conversation.getListParticipant()))
        }
        launch {
            localCache.conversationsDao()
                .updateMessagesCount(conversationSid, conversation.getMessageCount())
            localCache.messagesDao()
                .updateMessagesCount(conversationSid, conversation.getMessageCount())
        }
        launch {
            localCache.conversationsDao().updateUnreadMessagesCount(
                conversationSid,
                conversation.getUnreadMessageCount() ?: return@launch
            )
        }
        launch {
            updateConversationLastMessage(conversationSid)
        }

        conversationsClientWrapper.getConversationsClient()
            .getConversation(conversationSid) { result ->
                launch {
                    val attributes = result.attributes.toString()
                    localCache.conversationsDao().updateAttribute(conversationSid, attributes)

                    val deleted = isDelete(attributes)

                    if (!deleted.isNullOrEmpty()) {
                        deleted.forEach {
                            if (it.identity == credentialStorage.identity && it.status) {
                                localCache.conversationsDao().delete(conversationSid)
                                return@forEach
                            }
                        }
                    }

                }
            }
    }

    private suspend fun updateConversationLastMessage(conversationSid: String) {
        val lastMessage = localCache.messagesDao().getLastMessage(conversationSid)
        if (lastMessage != null) {
            val typeDeletes = getTypeDelete(lastMessage.attributes).asTypeDelete()
            if (lastMessage.body.isNullOrEmpty() && lastMessage.type == 1) {
                when (typeDeletes.type) {
                    TypeDeleteMessage.TYPE_DELETE_ALL.type -> {
                        localCache.conversationsDao().updateLastMessage(
                            conversationSid,
                            "Message has been deleted.",
                            lastMessage.sendStatus,
                            lastMessage.dateCreated,
                            lastMessage.index
                        )
                    }
                    TypeDeleteMessage.TYPE_DELETE_FOR_YOU.type -> {
                        if (typeDeletes.identity == credentialStorage.identity) {
                            localCache.conversationsDao().updateLastMessage(
                                conversationSid,
                                "Message has been deleted.",
                                lastMessage.sendStatus,
                                lastMessage.dateCreated,
                                lastMessage.index
                            )
                        } else {
                            localCache.conversationsDao().updateLastMessage(
                                conversationSid,
                                lastMessage.mediaFileName ?: "Attachment",
                                lastMessage.sendStatus,
                                lastMessage.dateCreated,
                                lastMessage.index
                            )
                        }
                    }
                    TypeDeleteMessage.TYPE_DELETE_DEFAULT.type -> {
                        localCache.conversationsDao().updateLastMessage(
                            conversationSid,
                            lastMessage.mediaFileName ?: "Attachment",
                            lastMessage.sendStatus,
                            lastMessage.dateCreated,
                            lastMessage.index
                        )
                    }
                }


            } else {
                when (typeDeletes.type) {
                    TypeDeleteMessage.TYPE_DELETE_ALL.type -> {
                        localCache.conversationsDao().updateLastMessage(
                            conversationSid,
                            "Message has been deleted.",
                            lastMessage.sendStatus,
                            lastMessage.dateCreated,
                            lastMessage.index
                        )
                    }
                    TypeDeleteMessage.TYPE_DELETE_FOR_YOU.type -> {
                        if (typeDeletes.identity == credentialStorage.identity) {
                            localCache.conversationsDao().updateLastMessage(
                                conversationSid,
                                "Message has been deleted.",
                                lastMessage.sendStatus,
                                lastMessage.dateCreated,
                                lastMessage.index
                            )
                        } else {
                            localCache.conversationsDao().updateLastMessage(
                                conversationSid,
                                lastMessage.body ?: "Unknown",
                                lastMessage.sendStatus,
                                lastMessage.dateCreated,
                                lastMessage.index
                            )
                        }
                    }
                    TypeDeleteMessage.TYPE_DELETE_DEFAULT.type -> {
                        localCache.conversationsDao().updateLastMessage(
                            conversationSid,
                            lastMessage.body ?: "Unknown",
                            lastMessage.sendStatus,
                            lastMessage.dateCreated,
                            lastMessage.index
                        )
                    }
                }
            }

        } else {
            fetchMessages(conversationSid) { getLastMessages(10) }.collect()
        }
    }

    private fun deleteMessage(message: Message) {
        launch {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            localCache.messagesDao().delete(message.toMessageDataItem(identity))
            updateConversationLastMessage(message.conversationSid)
        }
    }

    private fun updateMessage(message: Message, updateReason: Message.UpdateReason? = null) {
        launch {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            val uuid = localCache.messagesDao().getMessageBySid(message.sid)?.uuid ?: ""
            localCache.messagesDao().insertOrReplace(message.toMessageDataItem(identity, uuid))
            updateConversationLastMessage(message.conversationSid)
        }
    }

    private fun addMessage(message: Message) {
        launch {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            localCache.messagesDao().updateByUuidOrInsert(
                message.toMessageDataItem(
                    identity,
                    message.attributes.string ?: ""
                )
            )
            updateConversationLastMessage(message.conversationSid)
        }
    }

    companion object {
        val INSTANCE
            get() = _instance ?: error("call ConversationsRepository.createInstance() first")

        private var _instance: ConversationsRepository? = null

        fun createInstance(
            conversationsClientWrapper: ConversationsClient,
            localCache: LocalCacheProvider,
            credentialStorage: CredentialStorage
        ) {
            check(_instance == null) { "ConversationsRepository singleton instance has been already created" }
            _instance = ConversationsRepositoryImpl(
                conversationsClientWrapper,
                localCache, credentialStorage
            )
        }
    }
}
