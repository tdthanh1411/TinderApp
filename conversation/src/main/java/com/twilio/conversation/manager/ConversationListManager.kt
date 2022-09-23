package com.twilio.conversation.manager

import android.util.Log
import com.google.gson.Gson
import com.twilio.conversation.common.extensions.*
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.model.conversation.ConversationAttributes
import com.twilio.conversation.data.model.conversation.Deleted
import com.twilio.conversations.Attributes
import org.json.JSONObject

interface ConversationListManager {
    suspend fun createConversation(friendlyName: String): String
    suspend fun checkConversation(friendlyName: String): String
    suspend fun joinConversation(conversationSid: String)
    suspend fun removeConversation(conversationSid: String)
    suspend fun leaveConversation(conversationSid: String)
    suspend fun muteConversation(conversationSid: String)
    suspend fun unmuteConversation(conversationSid: String)
    suspend fun renameConversation(conversationSid: String, friendlyName: String)
    suspend fun setAttributes(
        conversationSid: String,
        idSend: Boolean,
        identity: String,
        deleted: List<Deleted>,
        messageIndex: Long,
        isUploadAvatar: Boolean,
        urlAvatar: String
    )
}


class ConversationListManagerImpl(private val conversationsClient: ConversationsClient) :
    ConversationListManager {
    override suspend fun checkConversation(friendlyName: String): String =
        conversationsClient.getConversationsClient().checkConversationFromSidOrUniqueName(friendlyName)


    override suspend fun createConversation(friendlyName: String): String =
        conversationsClient.getConversationsClient().createConversation(friendlyName).sid

    override suspend fun joinConversation(conversationSid: String): Unit =
        conversationsClient.getConversationsClient().getConversation(conversationSid).join()

    override suspend fun removeConversation(conversationSid: String): Unit =
        conversationsClient.getConversationsClient().getConversation(conversationSid).destroy()

    override suspend fun leaveConversation(conversationSid: String): Unit =
        conversationsClient.getConversationsClient().getConversation(conversationSid).leave()

    override suspend fun muteConversation(conversationSid: String): Unit =
        conversationsClient.getConversationsClient().getConversation(conversationSid)
            .muteConversation()

    override suspend fun unmuteConversation(conversationSid: String): Unit =
        conversationsClient.getConversationsClient().getConversation(conversationSid)
            .unmuteConversation()

    override suspend fun renameConversation(conversationSid: String, friendlyName: String) =
        conversationsClient.getConversationsClient().getConversation(conversationSid)
            .setFriendlyName(friendlyName)

    override suspend fun setAttributes(
        conversationSid: String,
        idSend: Boolean,
        identity: String,
        deleted: List<Deleted>,
        messageIndex: Long,
        isUploadAvatar: Boolean,
        urlAvatar: String
    ) {
        val conversation =
            conversationsClient.getConversationsClient().getConversation(conversationSid)
        if (isUploadAvatar) {
            val conversationAttributes = ConversationAttributes(urlAvatar, deleted.toMutableList())
            conversation.setAttributes(Attributes(JSONObject(Gson().toJson(conversationAttributes))))
        } else {
            var isAddListDelete = true
            var listDataDelete: MutableList<Deleted> = arrayListOf()
            listDataDelete.clear()
            if (idSend) {
                listDataDelete = deleted.toMutableList()

            } else {
                if (deleted.isEmpty()) {
                    listDataDelete = mutableListOf(Deleted(messageIndex, true, identity))
                } else {

                    deleted.forEach { delete ->
                        if (delete.identity == identity) {
                            isAddListDelete = false
                            listDataDelete.add(Deleted(messageIndex, true, identity))
                        } else {
                            listDataDelete.add(
                                Deleted(
                                    delete.messageIndex,
                                    delete.status,
                                    delete.identity
                                )
                            )
                        }
                    }
                    if (isAddListDelete) {
                        listDataDelete.add(Deleted(messageIndex, true, identity))
                    }
                }
            }

            val conversationAttributes = ConversationAttributes(urlAvatar, listDataDelete)
            conversation.setAttributes(Attributes(JSONObject(Gson().toJson(conversationAttributes))))

        }


    }

}
