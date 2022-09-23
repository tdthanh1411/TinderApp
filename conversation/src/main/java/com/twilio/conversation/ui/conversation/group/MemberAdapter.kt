package com.twilio.conversation.ui.conversation.group

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.twilio.conversation.data.model.ParticipantListViewItem
import com.twilio.conversation.ui.conversation.newmessage.ParticipantDiffUtil

class MemberAdapter (private val listener: (ParticipantListViewItem) -> Unit) :
    ListAdapter<ParticipantListViewItem, MemberViewHolder>(ParticipantDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        return MemberViewHolder.from(parent, listener)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val memberItem = getItem(position) ?: null
        if (memberItem != null){
            holder.bind(memberItem)
        }
    }


}