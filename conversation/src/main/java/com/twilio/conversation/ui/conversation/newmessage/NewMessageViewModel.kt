package com.twilio.conversation.ui.conversation.newmessage

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twilio.conversation.common.SingleLiveEvent
import com.twilio.conversation.common.enums.ConversationsError
import com.twilio.conversation.common.extensions.ConversationsException
import com.twilio.conversation.common.extensions.getConversation
import com.twilio.conversation.common.extensions.setUniqueName
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.ParticipantListViewItem
import com.twilio.conversation.manager.ConversationListManager
import com.twilio.conversation.manager.ParticipantListManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by ThanhTran on 7/19/2022.
 */
@HiltViewModel
class NewMessageViewModel @Inject constructor(
    private val conversationsClient: ConversationsClient,
    private val conversationListManager: ConversationListManager,
    private val participantListManager: ParticipantListManager,
    private val credentialStorage: CredentialStorage
) : ViewModel() {
    var participantList: MutableList<ParticipantListViewItem> = arrayListOf(
        ParticipantListViewItem(
            sid = "MB5bcec82c30624017b99817b6db554906",
            identity = "thanh",
            conversationSid = "",
            friendlyName = "Thanh",
            isOnline = true
        ),
        ParticipantListViewItem(
            sid = "MBa670c4f04b6b433687e772b0b285567c",
            identity = "nghia6",
            conversationSid = "",
            friendlyName = "nghia6",
            isOnline = true
        ),
        ParticipantListViewItem(
            sid = "MBa670c4f04b6b433687e772b0b285567c",
            identity = "nghia3",
            conversationSid = "",
            friendlyName = "nghia3",
            isOnline = true
        )
    )
    private val _userData = MutableLiveData<MutableList<ParticipantListViewItem>>()
    val userData: LiveData<MutableList<ParticipantListViewItem>> get() = _userData


    private var dataMember: MutableList<ParticipantListViewItem> = arrayListOf()
    private val _memberSelectedData = MutableLiveData<MutableList<ParticipantListViewItem>>()
    val memberSelectedData: LiveData<MutableList<ParticipantListViewItem>> get() = _memberSelectedData

    val onCheckConversationCreated = SingleLiveEvent<Pair<String, String>>()
    val onCreateConversationWithUniqueNameWithFriendlyName = SingleLiveEvent<Unit>()

    val onConversationCreated = SingleLiveEvent<Pair<String, String>>()
    val onConversationGroupCreated = SingleLiveEvent<String>()

    val onParticipantAdded = SingleLiveEvent<Pair<String, String>>()

    val onAddParticipantError = SingleLiveEvent<ConversationsError>()
    val onConversationError = SingleLiveEvent<ConversationsError>()


    fun startConversation(identity: String, uniqueName: String) {
        viewModelScope.launch {
            try {
                val conversationSid = conversationListManager.checkConversation(uniqueName)
                onCheckConversationCreated.value = conversationSid to identity
            } catch (e: ConversationsException) {
                onConversationError.value = ConversationsError.CONVERSATION_GET_FAILED
            }
        }
    }

    fun createConversation(friendlyName: String, identity: String) = viewModelScope.launch {
        try {
            val conversationSid = conversationListManager.createConversation(friendlyName)
            conversationListManager.joinConversation(conversationSid)
            val conversation =
                conversationsClient.getConversationsClient().getConversation(conversationSid)
            conversation.setUniqueName(friendlyName)
            onConversationCreated.call(identity to conversationSid)
        } catch (e: ConversationsException) {
            onConversationError.value = ConversationsError.CONVERSATION_CREATE_FAILED
        } finally {

        }
    }


    //add Participant
    fun addChatParticipant(identity: String, conversationSid: String) = viewModelScope.launch {
        try {
            participantListManager.addChatParticipant(identity, conversationSid)
            onParticipantAdded.value = conversationSid to identity
        } catch (e: ConversationsException) {
            onAddParticipantError.value = ConversationsError.PARTICIPANT_ADD_FAILED
        } finally {

        }
    }


    fun checkConversationWithUniqueNameNoFriendlyName(uniqueName: String) =
        viewModelScope.launch {
            try {
                val conversationSid = conversationListManager.checkConversation(uniqueName)
                if (conversationSid != "Conversation not found") {
                    onCheckConversationCreated.value = conversationSid to ""
                } else {
                    createConversationGroup(uniqueName)
                }
            } catch (e: ConversationsException) {

            }
        }

    fun checkConversationWithUniqueNameWithFriendlyName(uniqueName: String, friendlyName: String) =
        viewModelScope.launch {
            try {
                val conversationSid = conversationListManager.checkConversation("$friendlyName-$uniqueName")
                if (conversationSid != "Conversation not found") {
                    onCheckConversationCreated.value = conversationSid to "identity"
                } else {
                    onCreateConversationWithUniqueNameWithFriendlyName.call()
                }

            } catch (e: ConversationsException) {

            }
        }


     fun createConversationGroup(uniqueName: String) = viewModelScope.launch {
        try {
            val conversationSid = conversationListManager.createConversation("")
            conversationListManager.joinConversation(conversationSid)
            val conversation =
                conversationsClient.getConversationsClient().getConversation(conversationSid)
            conversation.setUniqueName(uniqueName)
            onConversationGroupCreated.call(conversationSid)
        } catch (e: ConversationsException) {
            onConversationError.value = ConversationsError.CONVERSATION_CREATE_FAILED
        } finally {

        }
    }

     fun createConversationGroupWithFriendlyName(uniqueName: String, friendlyName: String) =
        viewModelScope.launch {
            try {
                val conversationSid =
                    conversationListManager.createConversation("$friendlyName")
                conversationListManager.joinConversation(conversationSid)
                val conversation =
                    conversationsClient.getConversationsClient().getConversation(conversationSid)
                conversation.setUniqueName("$friendlyName-$uniqueName")
                onConversationGroupCreated.call(conversationSid)
            } catch (e: ConversationsException) {
                onConversationError.value = ConversationsError.CONVERSATION_CREATE_FAILED
            } finally {

            }
        }


    //add Participant
    fun addParticipantGroupMessage(
        roomName: String,
        conversationSid: String,
        listParticipant: List<ParticipantListViewItem>
    ) = viewModelScope.launch {
        try {
            listParticipant.forEach {
                participantListManager.addChatParticipant(it.identity, conversationSid)
            }
            onParticipantAdded.value = conversationSid to ""
        } catch (e: ConversationsException) {
            onAddParticipantError.value = ConversationsError.PARTICIPANT_ADD_FAILED
        } finally {

        }
    }


    //add member selected
    fun addMemberSelected(participantListViewItem: ParticipantListViewItem) {
        dataMember.add(participantListViewItem)
        _memberSelectedData.postValue(dataMember)
    }

    //remove member selected
    fun removeMemberSelected(participantListViewItem: ParticipantListViewItem) {
        dataMember.remove(participantListViewItem)
        _memberSelectedData.postValue(dataMember)
    }


    //add member selected
    fun addMember(participantListViewItem: ParticipantListViewItem) {
        participantList.add(participantListViewItem)
        _userData.postValue(participantList)
    }

    //remove member selected
    fun removeMember(participantListViewItem: ParticipantListViewItem) {
        participantList.remove(participantListViewItem)
        _userData.postValue(participantList)
    }


}