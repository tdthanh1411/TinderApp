package com.twilio.conversation.ui.conversation.newmessage

import androidx.recyclerview.widget.DiffUtil
import com.twilio.conversation.data.model.ParticipantListViewItem

class ParticipantDiffUtil : DiffUtil.ItemCallback<ParticipantListViewItem>() {
        override fun areItemsTheSame(
            oldItem: ParticipantListViewItem,
            newItem: ParticipantListViewItem
        ) =
            oldItem.sid == newItem.sid

        override fun areContentsTheSame(
            oldItem: ParticipantListViewItem,
            newItem: ParticipantListViewItem
        ) = oldItem == newItem
    }