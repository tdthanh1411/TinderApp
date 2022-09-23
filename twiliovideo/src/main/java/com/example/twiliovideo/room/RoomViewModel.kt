package com.example.twiliovideo.room

import android.Manifest
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.annotation.VisibleForTesting.PROTECTED
import androidx.lifecycle.viewModelScope
import com.example.twiliovideo.sdk.RoomManager
import com.example.twiliovideo.sdk.VideoTrackViewState
import com.example.twiliovideo.room.RoomEvent.ConnectFailure
import com.example.twiliovideo.room.RoomEvent.Connected
import com.example.twiliovideo.room.RoomEvent.Connecting
import com.example.twiliovideo.room.RoomEvent.Disconnected
import com.example.twiliovideo.room.RoomEvent.DominantSpeakerChanged
import com.example.twiliovideo.room.RoomEvent.LocalParticipantEvent
import com.example.twiliovideo.room.RoomEvent.LocalParticipantEvent.AudioDisabled
import com.example.twiliovideo.room.RoomEvent.LocalParticipantEvent.AudioEnabled
import com.example.twiliovideo.room.RoomEvent.LocalParticipantEvent.AudioOff
import com.example.twiliovideo.room.RoomEvent.LocalParticipantEvent.AudioOn
import com.example.twiliovideo.room.RoomEvent.LocalParticipantEvent.ScreenCaptureOff
import com.example.twiliovideo.room.RoomEvent.LocalParticipantEvent.ScreenCaptureOn
import com.example.twiliovideo.room.RoomEvent.LocalParticipantEvent.VideoDisabled
import com.example.twiliovideo.room.RoomEvent.LocalParticipantEvent.VideoEnabled
import com.example.twiliovideo.room.RoomEvent.MaxParticipantFailure
import com.example.twiliovideo.room.RoomEvent.RecordingStarted
import com.example.twiliovideo.room.RoomEvent.RecordingStopped
import com.example.twiliovideo.room.RoomEvent.RemoteParticipantEvent
import com.example.twiliovideo.room.RoomEvent.RemoteParticipantEvent.MuteRemoteParticipant
import com.example.twiliovideo.room.RoomEvent.RemoteParticipantEvent.NetworkQualityLevelChange
import com.example.twiliovideo.room.RoomEvent.RemoteParticipantEvent.RemoteParticipantConnected
import com.example.twiliovideo.room.RoomEvent.RemoteParticipantEvent.RemoteParticipantDisconnected
import com.example.twiliovideo.room.RoomEvent.RemoteParticipantEvent.ScreenTrackUpdated
import com.example.twiliovideo.room.RoomEvent.RemoteParticipantEvent.TrackSwitchOff
import com.example.twiliovideo.room.RoomEvent.StatsUpdate
import com.example.twiliovideo.room.RoomViewConfiguration.Lobby
import com.example.twiliovideo.room.RoomViewEffect.PermissionsDenied
import com.example.twiliovideo.room.RoomViewEffect.ShowConnectFailureDialog
import com.example.twiliovideo.room.RoomViewEffect.ShowMaxParticipantFailureDialog
import com.example.twiliovideo.room.RoomViewEvent.ActivateAudioDevice
import com.example.twiliovideo.room.RoomViewEvent.Connect
import com.example.twiliovideo.room.RoomViewEvent.DeactivateAudioDevice
import com.example.twiliovideo.room.RoomViewEvent.DisableLocalAudio
import com.example.twiliovideo.room.RoomViewEvent.DisableLocalVideo
import com.example.twiliovideo.room.RoomViewEvent.Disconnect
import com.example.twiliovideo.room.RoomViewEvent.EnableLocalAudio
import com.example.twiliovideo.room.RoomViewEvent.EnableLocalVideo
import com.example.twiliovideo.room.RoomViewEvent.OnPause
import com.example.twiliovideo.room.RoomViewEvent.OnResume
import com.example.twiliovideo.room.RoomViewEvent.PinParticipant
import com.example.twiliovideo.room.RoomViewEvent.ScreenTrackRemoved
import com.example.twiliovideo.room.RoomViewEvent.SelectAudioDevice
import com.example.twiliovideo.room.RoomViewEvent.StartScreenCapture
import com.example.twiliovideo.room.RoomViewEvent.StopScreenCapture
import com.example.twiliovideo.room.RoomViewEvent.SwitchCamera
import com.example.twiliovideo.room.RoomViewEvent.ToggleLocalAudio
import com.example.twiliovideo.room.RoomViewEvent.ToggleLocalVideo
import com.example.twiliovideo.room.RoomViewEvent.VideoTrackRemoved


import com.example.twiliovideo.participant.ParticipantManager
import com.example.twiliovideo.participant.buildParticipantViewState
import com.example.twiliovideo.util.PermissionUtil
import com.twilio.audioswitch.AudioSwitch
import com.twilio.video.Participant
import dagger.hilt.android.lifecycle.HiltViewModel
import io.uniflow.android.AndroidDataFlow
import io.uniflow.core.flow.data.UIState
import io.uniflow.core.flow.onState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val roomManager: RoomManager,
    private val audioSwitch: AudioSwitch,
    private val permissionUtil: PermissionUtil,
    private val participantManager: ParticipantManager = ParticipantManager(),
    initialViewState: RoomViewState = RoomViewState(participantManager.primaryParticipant)
) : AndroidDataFlow(defaultState = initialViewState) {

    private var permissionCheckRetry = false
    @VisibleForTesting(otherwise = PRIVATE)
    internal var roomManagerJob: Job? = null

    init {
        subscribeToRoomEvents()
    }

    @VisibleForTesting(otherwise = PROTECTED)
    public override fun onCleared() {
        super.onCleared()
        audioSwitch.stop()
        roomManagerJob?.cancel()
        processInput(Disconnect)
    }

    fun processInput(viewEvent: RoomViewEvent) {

        when (viewEvent) {
            OnResume -> checkPermissions()
            OnPause -> roomManager.onPause()
            is SelectAudioDevice -> {
                audioSwitch.selectDevice(viewEvent.device)
            }
            ActivateAudioDevice -> { audioSwitch.activate() }
            DeactivateAudioDevice -> { audioSwitch.deactivate() }
            is Connect -> {
                connect(viewEvent.token,viewEvent.roomName)
            }
            is PinParticipant -> {
                participantManager.changePinnedParticipant(viewEvent.sid)
                updateParticipantViewState()
            }
            ToggleLocalVideo -> roomManager.toggleLocalVideo()
            EnableLocalVideo -> roomManager.enableLocalVideo()
            DisableLocalVideo -> roomManager.disableLocalVideo()
            ToggleLocalAudio -> roomManager.toggleLocalAudio()
            EnableLocalAudio -> roomManager.enableLocalAudio()
            DisableLocalAudio -> roomManager.disableLocalAudio()
            is StartScreenCapture -> roomManager.startScreenCapture(
                    viewEvent.captureResultCode, viewEvent.captureIntent)
            StopScreenCapture -> roomManager.stopScreenCapture()
            SwitchCamera -> roomManager.switchCamera()
            is VideoTrackRemoved -> {
                participantManager.updateParticipantVideoTrack(viewEvent.sid, null)
                updateParticipantViewState()
            }
            is ScreenTrackRemoved -> {
                participantManager.updateParticipantScreenTrack(viewEvent.sid, null)
                updateParticipantViewState()
            }
            Disconnect -> roomManager.disconnect()
        }
    }

    private fun subscribeToRoomEvents() {
        roomManager.roomEvents.let { sharedFlow ->
            roomManagerJob = viewModelScope.launch {
                sharedFlow.collect { observeRoomEvents(it) }
            }
        }
    }

    private fun checkPermissions() {
        val isCameraEnabled = permissionUtil.isPermissionGranted(Manifest.permission.CAMERA)
        val isMicEnabled = permissionUtil.isPermissionGranted(Manifest.permission.RECORD_AUDIO)

        updateState { currentState ->
            currentState.copy(isCameraEnabled = isCameraEnabled, isMicEnabled = isMicEnabled)
        }
        if (isCameraEnabled && isMicEnabled) {
            // start audio switch, it will silently error if it has already been started
            audioSwitch.start { audioDevices, selectedDevice ->
                updateState { currentState ->
                    currentState.copy(
                        selectedDevice = selectedDevice,
                        availableAudioDevices = audioDevices
                    )
                }
            }
            // resume everything else
            roomManager.onResume()
        } else {
            if (!permissionCheckRetry) {
                action {
                    sendEvent {
                        permissionCheckRetry = true
                        PermissionsDenied
                    }
                }
            }
        }
    }

    private fun observeRoomEvents(roomEvent: RoomEvent) {
        when (roomEvent) {
            is Connecting -> {
                showConnectingViewState()
            }
            is Connected -> {
                showConnectedViewState(roomEvent.roomName)
                checkParticipants(roomEvent.participants)
                action { sendEvent { RoomViewEffect.Connected(roomEvent.room) } }
            }
            is Disconnected -> showLobbyViewState()
            is DominantSpeakerChanged -> {
                participantManager.changeDominantSpeaker(roomEvent.newDominantSpeakerSid)
                updateParticipantViewState()
            }
            is ConnectFailure -> action {
                sendEvent {
                    showLobbyViewState()
                    ShowConnectFailureDialog
                }
            }
            is MaxParticipantFailure -> action {
                sendEvent { ShowMaxParticipantFailureDialog }
                showLobbyViewState()
            }

            RecordingStarted -> updateState { currentState -> currentState.copy(isRecording = true) }
            RecordingStopped -> updateState { currentState -> currentState.copy(isRecording = false) }
            is RemoteParticipantEvent -> handleRemoteParticipantEvent(roomEvent)
            is LocalParticipantEvent -> handleLocalParticipantEvent(roomEvent)
            is StatsUpdate -> updateState { currentState -> currentState.copy(roomStats = roomEvent.roomStats) }
        }
    }

    private fun handleRemoteParticipantEvent(remoteParticipantEvent: RemoteParticipantEvent) {
        when (remoteParticipantEvent) {
            is RemoteParticipantConnected -> addParticipant(remoteParticipantEvent.participant)
            is RemoteParticipantEvent.VideoTrackUpdated -> {
                participantManager.updateParticipantVideoTrack(remoteParticipantEvent.sid,
                        remoteParticipantEvent.videoTrack?.let { VideoTrackViewState(it) })
                updateParticipantViewState()
            }
            is TrackSwitchOff -> {
                participantManager.updateParticipantVideoTrack(remoteParticipantEvent.sid,
                        VideoTrackViewState(remoteParticipantEvent.videoTrack,
                                remoteParticipantEvent.switchOff))
                updateParticipantViewState()
            }
            is ScreenTrackUpdated -> {
                participantManager.updateParticipantScreenTrack(remoteParticipantEvent.sid,
                        remoteParticipantEvent.screenTrack?.let { VideoTrackViewState(it) })
                updateParticipantViewState()
            }
            is MuteRemoteParticipant -> {
                participantManager.muteParticipant(remoteParticipantEvent.sid,
                        remoteParticipantEvent.mute)
                updateParticipantViewState()
            }
            is NetworkQualityLevelChange -> {
                participantManager.updateNetworkQuality(remoteParticipantEvent.sid,
                        remoteParticipantEvent.networkQualityLevel)
                updateParticipantViewState()
            }
            is RemoteParticipantDisconnected -> {
                participantManager.removeParticipant(remoteParticipantEvent.sid)
                updateParticipantViewState()
            }
        }
    }

    private fun handleLocalParticipantEvent(localParticipantEvent: LocalParticipantEvent) {
        when (localParticipantEvent) {
            is LocalParticipantEvent.VideoTrackUpdated -> {
                participantManager.updateLocalParticipantVideoTrack(
                        localParticipantEvent.videoTrack?.let { VideoTrackViewState(it) })
                updateParticipantViewState()
                updateState { currentState -> currentState.copy(isVideoOff = localParticipantEvent.videoTrack == null) }
            }
            AudioOn -> updateState { currentState -> currentState.copy(isAudioMuted = false) }
            AudioOff -> updateState { currentState -> currentState.copy(isAudioMuted = true) }
            AudioEnabled -> updateState { currentState -> currentState.copy(isAudioEnabled = true) }
            AudioDisabled -> updateState { currentState -> currentState.copy(isAudioEnabled = false) }
            ScreenCaptureOn -> updateState { currentState -> currentState.copy(isScreenCaptureOn = true) }
            ScreenCaptureOff -> updateState { currentState -> currentState.copy(isScreenCaptureOn = false) }
            VideoEnabled -> updateState { currentState -> currentState.copy(isVideoEnabled = true) }
            VideoDisabled -> updateState { currentState -> currentState.copy(isVideoEnabled = false) }
        }
    }

    private fun addParticipant(participant: Participant) {
        val participantViewState = buildParticipantViewState(participant)
        participantManager.addParticipant(participantViewState)
        updateParticipantViewState()
    }

    private fun showLobbyViewState() {
        action { sendEvent { RoomViewEffect.Disconnected } }
        updateState { currentState ->
            currentState.copy(configuration = Lobby)
        }
        participantManager.clearRemoteParticipants()
        updateParticipantViewState()
    }

    private fun showConnectingViewState() {
        updateState { currentState ->
            currentState.copy(configuration = RoomViewConfiguration.Connecting)
        }
    }

    private fun showConnectedViewState(roomName: String) {
        updateState { currentState ->
            currentState.copy(configuration = RoomViewConfiguration.Connected, title = roomName)
        }
    }

    private fun checkParticipants(participants: List<Participant>) {
        for ((index, participant) in participants.withIndex()) {
            if (index == 0) { // local participant
                participantManager.updateLocalParticipantSid(participant.sid)
            } else {
                participantManager.addParticipant(buildParticipantViewState(participant))
            }
        }
        updateParticipantViewState()
    }

    private fun updateParticipantViewState() {
        updateState { currentState ->
            currentState.copy(
                    participantThumbnails = participantManager.participantThumbnails,
                    primaryParticipant = participantManager.primaryParticipant
            )
        }
    }

    private fun connect(token: String, roomName: String) =
            viewModelScope.launch {

                roomManager.connect(
                        token,
                        roomName)
            }

    private fun updateState(action: (currentState: RoomViewState) -> UIState) =
            action { onState<RoomViewState> { currentState -> setState { action(currentState) } } }

}