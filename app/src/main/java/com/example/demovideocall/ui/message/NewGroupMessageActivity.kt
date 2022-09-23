package com.example.demovideocall.ui.message

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.twilio.conversation.R
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.ParticipantListViewItem
import com.twilio.conversation.data.model.conversation.ConversationListViewItem
import com.twilio.conversation.databinding.ActivityNewGroupMessageBinding
import com.twilio.conversation.ui.conversation.group.MemberAdapter
import com.twilio.conversation.ui.conversation.group.selected.MemberSelectedAdapter
import com.twilio.conversation.ui.conversation.newmessage.NewMessageViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class NewGroupMessageActivity : AppCompatActivity() {

    @Inject
    lateinit var credentialStorage: CredentialStorage

    companion object {

        const val EXTRA_CONVERSATION_DATA = "EXTRA_CONVERSATION_DATA"

        fun start(
            context: Context,
            conversationData: List<ConversationListViewItem>
        ) = context.startActivity(Intent(context, NewGroupMessageActivity::class.java).apply {
            putExtra(EXTRA_CONVERSATION_DATA, conversationData as Serializable)
        })
    }

    private val newMessageViewModel: NewMessageViewModel by viewModels()

    private lateinit var binding: ActivityNewGroupMessageBinding
    private lateinit var memberAdapter: MemberAdapter
    private lateinit var memberSelectedAdapter: MemberSelectedAdapter

    private var dataSelected: MutableList<ParticipantListViewItem> = arrayListOf()

    private val conversationData by lazy { intent.getSerializableExtra(EXTRA_CONVERSATION_DATA) }

    var uniqueName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewGroupMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        initViewModelLifecycle()
        initListener()

    }

    private fun updateConversation(item: List<ParticipantListViewItem>) {
        val friendlyName = arrayListOf(credentialStorage.identity)
        dataSelected.forEach {
            friendlyName.add(it.identity)
        }
        val friendlyNameSorted = friendlyName.sortedBy { it }
        var nameRoom = ""
        for (i in friendlyNameSorted.indices) {
            nameRoom += if (i == friendlyNameSorted.indices.last) {
                friendlyNameSorted[i]
            } else {
                "${friendlyNameSorted[i]}-"
            }
        }
        uniqueName = nameRoom

        val participantCheck: MutableList<String> = arrayListOf()
        participantCheck.clear()

        var isNewConversation = true

        (conversationData as List<ConversationListViewItem>).forEach { conversationListViewItem ->
            conversationListViewItem.participantItem.sortedBy { it.identity }
                .forEach { participantDataItem ->
                    participantCheck.add(participantDataItem.identity)
                }
            if (participantCheck == friendlyNameSorted) {
                isNewConversation = false
                // start message activity
                if (conversationListViewItem.name.isNullOrEmpty()) {
                    newMessageViewModel.checkConversationWithUniqueNameNoFriendlyName(nameRoom)
                } else {
                    newMessageViewModel.checkConversationWithUniqueNameWithFriendlyName(
                        nameRoom,
                        conversationListViewItem.name
                    )
                }
                return@forEach
            }
        }
        if (isNewConversation) {
            newMessageViewModel.createConversationGroup(nameRoom)
            // cre conversation
        }
    }

    private fun initListener() {
        with(binding) {
            tvNext.setOnClickListener {
                updateConversation(dataSelected)
                binding.progressBarNewMessage.isVisible = true
                tvNext.isClickable = false
            }
            imgBack.setOnClickListener {
                onBackPressed()
            }
            edtNameRoom.addTextChangedListener {
                if (!it.toString().trim().isNullOrEmpty()) {
                    tvCreated.setTypeface(tvCreated.typeface, Typeface.BOLD)
                    tvCreated.isClickable = true
                }
            }
            tvCreated.setOnClickListener {
                binding.progressBarNewMessage.isVisible = true
                newMessageViewModel.createConversationGroupWithFriendlyName(
                    uniqueName,
                    binding.edtNameRoom.text.toString()
                )

            }
        }
    }

    private fun initViewModelLifecycle() {
        newMessageViewModel.userData.observe(this) {
            memberAdapter.submitList(it)
            memberAdapter.notifyDataSetChanged()
        }

        newMessageViewModel.onConversationError.observe(this) {
            binding.progressBarNewMessage.isVisible = false
            onBackPressed()
            Toast.makeText(this, R.string.txt_create_failed, Toast.LENGTH_SHORT).show()
        }
        newMessageViewModel.onCheckConversationCreated.observe(this) {
            binding.progressBarNewMessage.isVisible = false
            startNewMessage(it.first, it.second)
        }
        newMessageViewModel.onCreateConversationWithUniqueNameWithFriendlyName.observe(this) {
            with(binding) {
                tvNext.isVisible = false
                tvCreated.isVisible = true
                edtNameRoom.isVisible = true
                tvCreated.setTypeface(null, Typeface.NORMAL)
                tvCreated.isClickable = false
                progressBarNewMessage.isVisible = false

            }
        }

        newMessageViewModel.onConversationGroupCreated.observe(this) {
            newMessageViewModel.addParticipantGroupMessage("", it, dataSelected)
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

        newMessageViewModel.memberSelectedData.observe(this@NewGroupMessageActivity) {
            dataSelected = it
            memberSelectedAdapter.submitList(it)
            memberSelectedAdapter.notifyDataSetChanged()
            if (it.size >= 2) {
                binding.tvNext.isClickable = true
                binding.tvNext.setTypeface(binding.tvNext.typeface, Typeface.BOLD)
            } else {
                binding.tvNext.setTypeface(null, Typeface.NORMAL)
                binding.tvNext.isClickable = false
            }
        }
    }

    private fun initData() {
        binding.tvNext.setTypeface(null, Typeface.NORMAL)
        binding.tvNext.isClickable = false

        with(binding) {
            memberAdapter = MemberAdapter {
                newMessageViewModel.removeMember(it)
                newMessageViewModel.addMemberSelected(it)
            }
            recyclerViewMember.adapter = memberAdapter
            memberAdapter.submitList(newMessageViewModel.participantList)

            //setup user selected
            memberSelectedAdapter = MemberSelectedAdapter {
                newMessageViewModel.removeMemberSelected(it)
                newMessageViewModel.addMember(it)
            }
            binding.recyclerViewItemSelected.adapter = memberSelectedAdapter

        }
    }

    private fun startNewMessage(conversationSid: String, conversationName: String) {
        MessageActivity.start(this, conversationSid, conversationName, 0L, 3, arrayListOf())
        finish()
    }


}