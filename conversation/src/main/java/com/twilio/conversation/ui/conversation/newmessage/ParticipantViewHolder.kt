package com.twilio.conversation.ui.conversation.newmessage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversation.data.model.ParticipantListViewItem
import com.twilio.conversation.data.model.localCache.entity.ParticipantDataItem
import com.twilio.conversation.databinding.ItemUserNewMessageBinding

/**
 * Created by ThanhTran on 7/19/2022.
 */
class ParticipantViewHolder private constructor(
    private val binding: ItemUserNewMessageBinding,
    private val listener: (ParticipantListViewItem) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun from(
            parent: ViewGroup,
            listener: (ParticipantListViewItem) -> Unit
        ): ParticipantViewHolder {
            val binding =
                ItemUserNewMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            return ParticipantViewHolder(binding, listener)
        }
    }

    fun bind(participantDataItem: ParticipantListViewItem) {
        with(binding) {
            radioButtonSelected.isVisible = false
            tvNameUser.text = participantDataItem.identity
            binding.root.setOnClickListener {
                listener(participantDataItem)
            }
        }
    }
}