package com.example.demovideocall.ui.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.twilio.conversation.R
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.ParticipantListViewItem
import com.twilio.conversation.data.model.conversation.ConversationListViewItem
import com.twilio.conversation.databinding.ActivityNewMessageBinding
import com.twilio.conversation.ui.conversation.newmessage.NewMessageViewModel
import com.twilio.conversation.ui.conversation.newmessage.ParticipantAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.io.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class NewMessageActivity : AppCompatActivity() {

    companion object {

        const val EXTRA_CONVERSATION_DATA = "EXTRA_CONVERSATION_DATA"

        fun start(
            context: Context,
            conversationData: List<ConversationListViewItem>
        ) = context.startActivity(Intent(context, NewMessageActivity::class.java).apply {
            putExtra(EXTRA_CONVERSATION_DATA, conversationData as Serializable)
        })
    }


    @Inject
    lateinit var credentialStorage: CredentialStorage

    private lateinit var binding: ActivityNewMessageBinding

    private lateinit var participantAdapter: ParticipantAdapter

    private val newMessageViewModel: NewMessageViewModel by viewModels()

    private val conversationData by lazy { intent.getSerializableExtra(EXTRA_CONVERSATION_DATA) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initConversationData()
        initData()
        initListener()
        initViewModelLifecycle()

    }

    private fun initConversationData() {


    }

    private fun initViewModelLifecycle() {
        newMessageViewModel.onConversationCreated.observe(this) {
            newMessageViewModel.addChatParticipant(it.first, it.second)
        }

        newMessageViewModel.onConversationError.observe(this) {
            binding.progressBarNewMessage.isVisible = false
            onBackPressed()
            Toast.makeText(this, R.string.txt_create_failed, Toast.LENGTH_SHORT).show()
        }

        newMessageViewModel.onAddParticipantError.observe(this) {
            binding.progressBarNewMessage.isVisible = false
            onBackPressed()
            Toast.makeText(this, R.string.txt_create_failed, Toast.LENGTH_SHORT).show()
        }
        newMessageViewModel.onParticipantAdded.observe(this) {
            binding.progressBarNewMessage.isVisible = false
            startNewMessage(it.first, it.second)
        }

        newMessageViewModel.onCheckConversationCreated.observe(this) {
            binding.progressBarNewMessage.isVisible = false
            startNewMessage(it.first, it.second)
        }
    }

    private fun initData() {
        val data = listOf(
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
            )
        )

        participantAdapter = ParticipantAdapter { item ->
            updateConversation(item)
            binding.progressBarNewMessage.isVisible = true
        }
        binding.recyclerviewListUser.adapter = participantAdapter
        participantAdapter.submitList(data)
    }

    private fun updateConversation(item: ParticipantListViewItem) {
        val friendlyName = arrayListOf(item.identity, credentialStorage.identity)
        val uniqueName = friendlyName.sortedBy { it }
        val name = "${uniqueName[0]}-${uniqueName[1]}"

        val participantCheck: MutableList<String> = arrayListOf()
        participantCheck.clear()

        var isNewConversation = true

        (conversationData as List<ConversationListViewItem>).forEach { conversationListViewItem ->
            conversationListViewItem.participantItem.sortedBy { it.identity }
                .forEach { participantDataItem ->
                    participantCheck.add(participantDataItem.identity)
                }

            if (participantCheck == uniqueName) {
                isNewConversation = false
                // start message activity
                newMessageViewModel.startConversation(item.identity, name)
                return@forEach
            }
        }
        if (isNewConversation) {
            // cre conversation
            newMessageViewModel.createConversation(name, item.identity)
        }
    }

    private fun initListener() {
        with(binding) {
            tvCancel.setOnClickListener {
                onBackPressed()
            }
            imgNewGroup.setOnClickListener {
                startNewGroupMessage()
            }
            tvNewGroup.setOnClickListener {
                startNewGroupMessage()
            }
            imgNewGroupNext.setOnClickListener {
                startNewGroupMessage()
            }
        }
    }

    private fun startNewMessage(conversationSid: String, conversationName: String) {
        MessageActivity.start(this, conversationSid, conversationName, 0L, 2, arrayListOf())
        finish()
    }

    private fun startNewGroupMessage() {
        NewGroupMessageActivity.start(this, (conversationData as List<ConversationListViewItem>))
    }
}