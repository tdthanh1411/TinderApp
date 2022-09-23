package com.example.twiliovideo.room

import com.example.twiliovideo.participant.ParticipantViewState
import com.example.twiliovideo.sdk.RoomStats
import com.example.twiliovideo.sdk.VideoTrackViewState
import com.twilio.audioswitch.AudioDevice
import com.example.twiliovideo.room.RoomViewConfiguration.Lobby
import io.uniflow.core.flow.data.UIState

data class RoomViewState(
    val primaryParticipant: ParticipantViewState,
    val title: String? = null,
    val participantThumbnails: List<ParticipantViewState>? = null,
    val selectedDevice: AudioDevice? = null,
    val availableAudioDevices: List<AudioDevice>? = null,
    val configuration: RoomViewConfiguration = Lobby,
    val isCameraEnabled: Boolean = false,
    val localVideoTrack: VideoTrackViewState? = null,
    val isMicEnabled: Boolean = false,
    val isAudioMuted: Boolean = false,
    val isAudioEnabled: Boolean = true,
    val isVideoEnabled: Boolean = true,
    val isVideoOff: Boolean = false,
    val isScreenCaptureOn: Boolean = false,
    val isRecording: Boolean = false,
    val roomStats: RoomStats? = null
) : UIState()

sealed class RoomViewConfiguration {
    object Connecting : RoomViewConfiguration()
    object Connected : RoomViewConfiguration()
    object Lobby : RoomViewConfiguration()
}
