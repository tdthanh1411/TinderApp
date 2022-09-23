package com.twilio.conversation.ui.conversation

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twilio.conversation.common.SingleLiveEvent
import com.twilio.conversation.common.asConversationListViewItems
import com.twilio.conversation.common.enums.ConversationsError
import com.twilio.conversation.common.extensions.ConversationsException
import com.twilio.conversation.common.merge
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.conversation.ConversationListViewItem
import com.twilio.conversation.data.model.RepositoryRequestStatus
import com.twilio.conversation.data.model.conversation.Deleted
import com.twilio.conversation.data.model.localCache.LocalCacheProvider
import com.twilio.conversation.manager.ConversationListManager
import com.twilio.conversation.repository.ConversationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by ThanhTran on 7/6/2022.
 */

@ExperimentalCoroutinesApi
@FlowPreview
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationsRepository: ConversationsRepository,
    private val conversationListManager: ConversationListManager,
    private val credentialStorage: CredentialStorage
) : ViewModel() {

    private val unfilteredUserConversationItems =
        MutableLiveData<List<ConversationListViewItem>>(emptyList())

    val userConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())

    val isDataLoading = SingleLiveEvent<Boolean>()
    val isNoResultsFoundVisible = MutableLiveData(false)
    val isNoConversationsVisible = MutableLiveData(false)

    val onConversationDelete = SingleLiveEvent<Unit>()
    val onConversationMuted = SingleLiveEvent<Boolean>()
    val onConversationError = SingleLiveEvent<ConversationsError>()

    var conversationFilter by Delegates.observable("") { _, _, _ -> updateUserConversationItems() }

    init {
        viewModelScope.launch {
            getUserConversations()
        }

        unfilteredUserConversationItems.observeForever { updateUserConversationItems() }
    }

    private fun updateUserConversationItems() {
        val filteredItems =
            unfilteredUserConversationItems.value?.filterByName(conversationFilter) ?: emptyList()
        userConversationItems.value = filteredItems

        isNoResultsFoundVisible.value = conversationFilter.isNotEmpty() && filteredItems.isEmpty()
        isNoConversationsVisible.value = conversationFilter.isEmpty() && filteredItems.isEmpty()
    }

    private fun getUserConversations() = viewModelScope.launch {
        conversationsRepository.getUserConversations().collect { (list, status) ->

            unfilteredUserConversationItems.value = list
                .asConversationListViewItems(context)
                .merge(unfilteredUserConversationItems.value)

            if (status is RepositoryRequestStatus.Error) {
                onConversationError.value = ConversationsError.CONVERSATION_FETCH_USER_FAILED
            }
        }
    }

    private fun setDataLoading(loading: Boolean) {
        if (isDataLoading.value != loading) {
            isDataLoading.value = loading
        }
    }

    private fun setConversationLoading(conversationSid: String, loading: Boolean) {
        fun ConversationListViewItem.transform() =
            if (sid == conversationSid) copy(isLoading = loading) else this
        unfilteredUserConversationItems.value =
            unfilteredUserConversationItems.value?.map { it.transform() }
    }

    private fun isConversationLoading(conversationSid: String): Boolean =
        unfilteredUserConversationItems.value?.find { it.sid == conversationSid }?.isLoading == true

    private fun List<ConversationListViewItem>.filterByName(name: String): List<ConversationListViewItem> =
        if (name.isEmpty()) {
            this
        } else {
            filter { it.name.contains(name, ignoreCase = true) }
        }


    fun muteConversation(conversationSid: String) = viewModelScope.launch {
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        try {
            setConversationLoading(conversationSid, true)
            conversationListManager.muteConversation(conversationSid)
            onConversationMuted.value = true
        } catch (e: ConversationsException) {
            onConversationError.value = ConversationsError.CONVERSATION_MUTE_FAILED
        } finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun unmuteConversation(conversationSid: String) = viewModelScope.launch {
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        try {
            setConversationLoading(conversationSid, true)
            conversationListManager.unmuteConversation(conversationSid)
            onConversationMuted.value = false
        } catch (e: ConversationsException) {
            onConversationError.value = ConversationsError.CONVERSATION_UNMUTE_FAILED
        } finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun deleteConversation(
        conversationSid: String,
        deleted: List<Deleted>,
        messageIndex: Long,
        avatarUrl: String
    ) =
        viewModelScope.launch {
            if (isConversationLoading(conversationSid)) {
                return@launch
            }
            try {
                setConversationLoading(conversationSid, true)
//                conversationListManager.setAttributes(
//                    conversationSid = conversationSid,
//                    idSend = false,
//                    identity = credentialStorage.identity,
//                    deleted = deleted,
//                    messageIndex = messageIndex,
//                    false,
//                    avatarUrl
//                )
                conversationListManager.removeConversation(conversationSid)
                onConversationDelete.call()
            } catch (e: ConversationsException) {
                onConversationError.value = ConversationsError.CONVERSATION_LEAVE_FAILED
            } finally {
                setConversationLoading(conversationSid, false)
            }
        }
}