package com.twilio.conversation.ui.conversation.newmessage

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.twilio.conversation.data.model.ParticipantListViewItem

/**
 * Created by ThanhTran on 7/19/2022.
 */
class ParticipantAdapter(private val listener: (ParticipantListViewItem) -> Unit) :
    ListAdapter<ParticipantListViewItem, ParticipantViewHolder>(ParticipantDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        return ParticipantViewHolder.from(parent, listener)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participantListViewItem = getItem(position) ?: null
        if (participantListViewItem != null){
            holder.bind(participantListViewItem)
        }
    }


}