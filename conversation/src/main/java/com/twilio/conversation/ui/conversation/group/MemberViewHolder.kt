package com.twilio.conversation.ui.conversation.group

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversation.data.model.ParticipantListViewItem
import com.twilio.conversation.databinding.ItemUserNewMessageBinding

class MemberViewHolder private constructor(
    private val binding: ItemUserNewMessageBinding,
    private val listener: (ParticipantListViewItem) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun from(
            parent: ViewGroup,
            listener: (ParticipantListViewItem) -> Unit
        ): MemberViewHolder {
            val binding =
                ItemUserNewMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            return MemberViewHolder(binding, listener)
        }
    }

    fun bind(participantDataItem: ParticipantListViewItem) {
        with(binding) {
            tvNameUser.text = participantDataItem.identity

            binding.root.setOnClickListener {
                listener(participantDataItem )
            }

            binding.radioButtonSelected.setOnClickListener {
                listener(participantDataItem)
            }
        }
    }
}