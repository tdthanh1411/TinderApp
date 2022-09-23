package com.twilio.conversation.ui.conversation.group.selected

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversation.data.model.ParticipantListViewItem
import com.twilio.conversation.databinding.ItemUserSelectBinding

class MemberSelectedViewHolder private constructor(
    private val binding: ItemUserSelectBinding,
    private val cancelListener: (ParticipantListViewItem) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun from(
            parent: ViewGroup,
            listener: (ParticipantListViewItem) -> Unit
        ): MemberSelectedViewHolder {
            val binding =
                ItemUserSelectBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            return MemberSelectedViewHolder(binding, listener)
        }
    }

    fun bind(participantDataItem: ParticipantListViewItem) {
        with(binding) {
            viewCancel.setOnClickListener {
                cancelListener(participantDataItem)
            }
        }
    }
}