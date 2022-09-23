package com.example.demovideocall.ui.conversation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.demovideocall.R
import com.example.demovideocall.ui.MainActivity
import com.example.demovideocall.ui.message.MessageActivity
import com.example.demovideocall.ui.message.NewMessageActivity
import com.twilio.conversation.ui.conversation.ConversationSwipeCallback
import com.twilio.conversation.adapter.ConversationAdapter
import com.twilio.conversation.adapter.OnConversationEvent
import com.twilio.conversation.common.SingleLiveEvent
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.conversation.ConversationListViewItem
import com.twilio.conversation.data.model.conversation.Deleted
import com.twilio.conversation.databinding.FragmentConversationsBinding
import com.twilio.conversation.ui.conversation.ConversationsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
@FlowPreview
@AndroidEntryPoint
class ConversationsFragment : Fragment(), OnConversationEvent {
    @Inject
    lateinit var credentialStorage: CredentialStorage


    private lateinit var _binding: FragmentConversationsBinding
    private val binding get() = _binding

    private val activity by lazy { requireActivity() as MainActivity }

    private val conversationsViewModel: ConversationsViewModel by viewModels()

    private val conversationAdapter by lazy {
        ConversationAdapter(
            this,
            credentialStorage,
            viewLifecycleOwner
        )
    }

    var listItemConversation: MutableList<ConversationListViewItem> = mutableListOf()
    private val onBadgeChange = SingleLiveEvent<Pair<Boolean, List<ConversationListViewItem>>>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        conversationsViewModel.userConversationItems.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                conversationAdapter.conversations = list
                listItemConversation.clear()
                listItemConversation.addAll(list)
                onBadgeChange.call(true to listItemConversation)
                binding.recyclerViewConversation.isVisible = true
                binding.shimmerFrameLayout.isVisible = false
            } else {
                binding.shimmerFrameLayout.startShimmer()
                binding.recyclerViewConversation.isVisible = false
                binding.shimmerFrameLayout.isVisible = true
            }


        }

        conversationsViewModel.isNoConversationsVisible.observe(viewLifecycleOwner) {
            //view empty visible
        }

        conversationsViewModel.isNoResultsFoundVisible.observe(viewLifecycleOwner) {
            // no result
        }

        onBadgeChange.observe(viewLifecycleOwner) { badgeChange ->
            if (badgeChange.first) {
                var countBadge = 0
                badgeChange.second.forEach { item ->
                    if (item.showUnreadMessageCount) {
                        countBadge++
                    }
                }
                if (countBadge != 0) {
                    activity.updateBadge(countBadge)
                } else {
                    activity.updateBadge(0)
                }
            }
        }

        with(binding) {
            //init adapter
            recyclerViewConversation.adapter = conversationAdapter
            val swipeCallback =
                ConversationSwipeCallback(requireContext(), conversationAdapter, credentialStorage)
            swipeCallback.onDelete = { conversationSid, deleted, messageIndex, avatarUrl ->
                showDeleteConversationDialog(conversationSid, deleted, messageIndex, avatarUrl)
            }
            swipeCallback.onMute = { conversationSid ->
                conversationsViewModel.muteConversation(conversationSid)
            }
            swipeCallback.onUnMute = { conversationSid ->
                conversationsViewModel.unmuteConversation(conversationSid)

            }
            val itemTouchHelper = ItemTouchHelper(swipeCallback)
            itemTouchHelper.attachToRecyclerView(recyclerViewConversation)




            imgNewMessage.setOnClickListener {
                startNewMessage()
            }

            edtSearch.addTextChangedListener { edittext ->
                if (edittext.isNullOrEmpty()) {
                    conversationAdapter.conversations = listItemConversation
                } else {
                    if (listItemConversation.isNotEmpty()) {
                        val filter = listItemConversation.filter { conversation ->
                            conversation.name.lowercase(Locale.US)
                                .contains(edittext.toString().lowercase(Locale.US))
                        }
                        conversationAdapter.conversations = filter
                    }
                }

            }
        }


    }

    private fun showDeleteConversationDialog(
        conversationSid: String,
        deleted: List<Deleted>,
        messageIndex: Long,
        avatarUrl: String
    ) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_dialog_title)
            .setMessage(R.string.delete_dialog_message)
            .setPositiveButton(R.string.close, null)
            .setNegativeButton(R.string.delete) { _, _ ->
                conversationsViewModel.deleteConversation(
                    conversationSid,
                    deleted = deleted,
                    messageIndex = messageIndex,
                    avatarUrl = avatarUrl
                )
            }
            .create()

        dialog.setOnShowListener {
            val color = ContextCompat.getColor(requireContext(), R.color.red)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)

            val colorButtonPositive = ContextCompat.getColor(requireContext(), R.color.black)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorButtonPositive)

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
        }

        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        binding.shimmerFrameLayout.stopShimmer()
    }

    private fun startNewMessage() {
        NewMessageActivity.start(activity, listItemConversation)

    }


    override fun onConversationClicked(
        conversationSid: String,
        conversationName: String,
        messageIndex: Long,
        participantCount: Int,
        avatarList: List<String>
    ) {
        MessageActivity.start(
            activity,
            conversationSid,
            conversationName,
            messageIndex,
            participantCount,
            avatarList
        )
    }


}