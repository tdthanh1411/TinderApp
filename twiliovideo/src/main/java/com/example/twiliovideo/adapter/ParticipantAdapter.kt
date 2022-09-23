package com.example.twiliovideo.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.example.twiliovideo.participant.ParticipantViewState

class ParticipantAdapter() :
    ListAdapter<ParticipantViewState, ParticipantViewHolder>(ParticipantDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        return ParticipantViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participantViewState = getItem(position) ?: null
        if (participantViewState!= null){
            holder.bind(participantViewState)
        }
    }
}