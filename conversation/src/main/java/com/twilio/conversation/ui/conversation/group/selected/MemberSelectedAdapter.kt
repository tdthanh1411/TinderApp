package com.twilio.conversation.ui.conversation.group.selected

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.twilio.conversation.data.model.ParticipantListViewItem
import com.twilio.conversation.ui.conversation.newmessage.ParticipantDiffUtil

class MemberSelectedAdapter (private val cancelListener: (ParticipantListViewItem) -> Unit) :
    ListAdapter<ParticipantListViewItem, MemberSelectedViewHolder>(ParticipantDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberSelectedViewHolder {
        return MemberSelectedViewHolder.from(parent, cancelListener)
    }

    override fun onBindViewHolder(holder: MemberSelectedViewHolder, position: Int) {
        val memberItem = getItem(position) ?: null
        if (memberItem != null){
            holder.bind(memberItem)
        }
    }


}