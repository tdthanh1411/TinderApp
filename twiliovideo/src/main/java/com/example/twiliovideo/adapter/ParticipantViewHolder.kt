package com.example.twiliovideo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.twiliovideo.R
import com.example.twiliovideo.databinding.ItemParticipantBinding
import com.example.twiliovideo.participant.ParticipantViewState
import com.example.twiliovideo.room.ParticipantThumbView
import com.example.twiliovideo.room.ParticipantView
import com.example.twiliovideo.sdk.VideoTrackViewState
import com.twilio.video.VideoTrack

class ParticipantViewHolder private constructor(
    val binding: ItemParticipantBinding
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        fun from(parent: ViewGroup): ParticipantViewHolder {
            val binding =
                ItemParticipantBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            return ParticipantViewHolder(binding)
        }
    }

    fun bind(participantViewState: ParticipantViewState) {
        with(binding) {
            thumbViewVideoView.run {
                val identity =
                    if (participantViewState.isLocalParticipant)
                        thumbViewVideoView.context.getString(R.string.you) else participantViewState.identity
                setIdentityy(identity ?: "")
                setMuted(participantViewState.isMuted)
                setPinned(participantViewState.isPinned)

                updateVideoTrack(participantViewState)
            }
        }


    }
    private fun updateVideoTrack(participantViewState: ParticipantViewState) {
        binding.thumbViewVideoView.run {
            val videoTrackViewState = participantViewState.videoTrack
            val newVideoTrack = videoTrackViewState?.videoTrack
            if (videoTrack !== newVideoTrack) {
                removeSink(videoTrack, this)
                videoTrack = newVideoTrack
                videoTrack?.let { videoTrack ->
                    setVideoState(videoTrackViewState)
                    if (videoTrack.isEnabled) videoTrack.addSink(this.getVideoTextureView())
                } ?: setStated(ParticipantView.State.NO_VIDEO)
            } else {
                setVideoState(videoTrackViewState)
            }
        }
    }

    private fun ParticipantThumbView.setVideoState(videoTrackViewState: VideoTrackViewState?) {
        if (videoTrackViewState?.isSwitchedOff == true) {
            setStated(ParticipantView.State.SWITCHED_OFF)
        } else {
            videoTrackViewState?.videoTrack?.let { setStated(ParticipantView.State.VIDEO) }
                ?: setStated(ParticipantView.State.NO_VIDEO)
        }
    }

    private fun removeSink(videoTrack: VideoTrack?, view: ParticipantView) {
        if (videoTrack == null || !videoTrack.sinks.contains(view.getVideoTextureView())) return
        videoTrack.removeSink(view.getVideoTextureView())
    }

}