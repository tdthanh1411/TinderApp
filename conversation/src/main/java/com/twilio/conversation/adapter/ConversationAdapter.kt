package com.twilio.conversation.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversation.avatarview.load.coil.loadImage
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.conversation.ConversationListViewItem
import com.twilio.conversation.databinding.ItemConversationBinding
import kotlin.properties.Delegates

class ConversationAdapter(
    private val callback: OnConversationEvent,
    private val credentialStorage: CredentialStorage,
    private val lifecycle: LifecycleOwner
) :
    RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {
    var conversations: List<ConversationListViewItem> by Delegates.observable(emptyList()) { _, old, new ->
        DiffUtil.calculateDiff(ConversationDiff(old, new)).dispatchUpdatesTo(this)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = conversations.size


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = conversations[position]


        if (conversation.participantCount > 2) {
            holder.itemView.isVisible = true
        } else {
            holder.itemView.isVisible = conversation.messageCount > 0
        }

        val participantCount = if (conversation.participantCount > 1) {
            conversation.participantCount - 1
        } else conversation.participantCount

        with(holder.binding) {
            if (conversation.avatarUrl.isNullOrEmpty()) {
                if (participantCount > 4) {
                    imgAvatar.loadImage(avatar.take(4)) {
                        crossfade(true)
                        crossfade(300)
                        lifecycle(lifecycle)
                    }
                } else {
                    imgAvatar.loadImage(avatar.take(participantCount)) {
                        crossfade(true)
                        crossfade(300)
                        lifecycle(lifecycle)
                    }
                }
            } else {
                imgAvatar.loadImage(listOf(conversation.avatarUrl))
            }

            imgConversationStateIcon.isVisible = conversation.isMuted

            if (conversation.participantCount > 2) {
                tvConversationName.text = if (conversation.name.isNullOrEmpty()) {
                    conversation.participantName
                } else {
                    conversation.name
                }
            } else {
                tvConversationName.text = conversation.participantName
            }

            if (conversation.isMuted) {
                tvConversationName.setTextColor(Color.parseColor("#606B85"))
            }

            imgMessageStateIcon.setImageResource(conversation.lastMessageStateIcon)
            imgMessageStateIcon.isVisible = conversation.lastMessageStateIcon != 0

            if (conversation.lastMessageText.startsWith(conversation.sid)) {
                tvMessage.text = "GifPhy!"
            } else {
                tvMessage.text = conversation.lastMessageText
            }
            tvMessage.setTextColor(conversation.lastMessageColor)

            tvDateTime.text = conversation.lastMessageDate

            if (conversation.showUnreadMessageCount) {
                tvMessage.setTypeface(tvMessage.typeface, Typeface.BOLD)
                tvConversationName.setTypeface(
                    tvConversationName.typeface,
                    Typeface.BOLD
                )
            } else {
                tvMessage.setTypeface(null, Typeface.NORMAL)
                tvConversationName.setTypeface(null, Typeface.NORMAL)
            }

            root.setOnClickListener {
                var messageIndex = 0L
                if (!conversation.deleted.isNullOrEmpty()) {
                    conversation.deleted.forEach {
                        if (it.identity == credentialStorage.identity && !it.status) {
                            messageIndex = it.messageIndex
                            return@forEach
                        }
                    }
                }
                conversation.sid.let {
                    callback.onConversationClicked(
                        it,
                        conversation.name,
                        messageIndex,
                        conversation.participantCount,
                        if (conversation.avatarUrl.isNullOrEmpty()) {
                            avatar.take(participantCount)
                        } else {
                            listOf(conversation.avatarUrl)
                        }
                    )
                }
            }

        }


    }

    fun isMuted(position: Int) = conversations[position].isMuted

    class ViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root)

    class ConversationDiff(
        private val oldItems: List<ConversationListViewItem>,
        private val newItems: List<ConversationListViewItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].sid == newItems[newItemPosition].sid
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}

interface OnConversationEvent {

    fun onConversationClicked(
        conversationSid: String,
        conversationName: String,
        messageIndex: Long,
        participantCount: Int,
        avatarList: List<String>
    )
}

val avatar
    get() = listOf(
        "https://swiftype-ss.imgix.net/https%3A%2F%2Fcdn.petcarerx.com%2FLPPE%2Fimages%2Farticlethumbs%2FFluffy-Cats-Small.jpg?ixlib=rb-1.1.0&h=320&fit=clip&dpr=2.0&s=c81a75f749ea4ed736b7607100cb52cc.png",
        "https://images.ctfassets.net/cnu0m8re1exe/1GxSYi0mQSp9xJ5svaWkVO/d151a93af61918c234c3049e0d6393e1/93347270_cat-1151519_1280.jpg?fm=jpg&fl=progressive&w=660&h=433&fit=fill",
        "https://img.webmd.com/dtmcms/live/webmd/consumer_assets/site_images/article_thumbnails/other/cat_relaxing_on_patio_other/1800x1200_cat_relaxing_on_patio_other.jpg",
        "https://post.healthline.com/wp-content/uploads/2020/08/cat-thumb2-732x415.jpg",
    ).shuffled()