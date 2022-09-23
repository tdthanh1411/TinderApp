package com.example.twiliovideo.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.twiliovideo.participant.ParticipantViewState

class ParticipantDiffUtil : DiffUtil.ItemCallback<ParticipantViewState>() {
    override fun areItemsTheSame(
        oldItem: ParticipantViewState,
        newItem: ParticipantViewState
    ) =
        oldItem.sid == newItem.sid

    override fun areContentsTheSame(
        oldItem: ParticipantViewState,
        newItem: ParticipantViewState
    ) = oldItem == newItem


    override fun getChangePayload(
        oldItem: ParticipantViewState,
        newItem: ParticipantViewState
    ): Any? {
        return newItem
    }
}