package com.twilio.conversation.ui.message

import androidx.recyclerview.widget.DiffUtil
import com.twilio.conversation.data.model.message.MessageListViewItem

class MessageDiffUtil : DiffUtil.ItemCallback<MessageListViewItem>() {
    override fun areContentsTheSame(
        oldItem: MessageListViewItem,
        newItem: MessageListViewItem
    ) =
        oldItem == newItem

    override fun areItemsTheSame(
        oldItem: MessageListViewItem,
        newItem: MessageListViewItem
    ) =
        oldItem.sid == newItem.sid
}