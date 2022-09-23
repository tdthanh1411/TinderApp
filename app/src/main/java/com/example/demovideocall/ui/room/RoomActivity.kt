package com.example.demovideocall.ui.room


import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.view.animation.TranslateAnimation
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.demovideocall.R
import com.example.demovideocall.databinding.ActivityRoomBinding
import com.example.twiliovideo.adapter.ParticipantAdapter
import com.example.twiliovideo.participant.ParticipantViewState
import com.example.twiliovideo.room.*
import com.example.twiliovideo.room.RoomViewEffect.*
import com.example.twiliovideo.room.RoomViewEvent.*
import com.example.twiliovideo.sdk.VideoTrackViewState
import com.example.twiliovideo.util.convertTimeToHHMMSS
import com.example.twiliovideo.util.convertTimeToMMSS
import com.twilio.video.VideoTrack
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.uniflow.android.livedata.onEvents
import io.uniflow.android.livedata.onStates
import java.util.concurrent.TimeUnit


@AndroidEntryPoint
class RoomActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100

        private const val ACTION_CONTROL = "ACTION_CONTROL"
        private const val EXTRA_CONTROL_TYPE = "EXTRA_CONTROL_TYPE"
        private const val CONTROL_TYPE_END = 1
        private const val CONTROL_TYPE_VIDEO = 2
        private const val CONTROL_TYPE_MIC = 3

    }

    private lateinit var binding: ActivityRoomBinding

    private val roomViewModel: RoomViewModel by viewModels()

    private var savedVolumeControlStream = 0

    private lateinit var primaryParticipantController: PrimaryParticipantController

    private var isEndCall = 1
    private var disposable = CompositeDisposable()
    private var isDisposable = true

    private var isHide = true
    var time = "00:00"


    private lateinit var participantAdapter: ParticipantAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivityRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        connectToRoom()
        setupRecyclerView()


        // Cache volume control stream
        savedVolumeControlStream = volumeControlStream

        // Setup participant controller
        primaryParticipantController = PrimaryParticipantController(binding.primaryVideoView)


        onStates(roomViewModel) { state ->
            if (state is RoomViewState) bindRoomViewState(state)

        }
        onEvents(roomViewModel) { event ->
            if (event is RoomViewEffect) bindRoomViewEffects(event)
        }


        listenerView()

        registerReceiver(broadcastReceiver, IntentFilter(ACTION_CONTROL))
    }

    private fun setupRecyclerView() {
        participantAdapter = ParticipantAdapter()
        binding.recyclerViewParticipant.adapter = participantAdapter
    }


    private fun bindRoomViewState(roomViewState: RoomViewState) {
        renderPrimaryView(roomViewState.primaryParticipant)
        renderThumbnails(roomViewState)
        updateLayout(roomViewState)

    }

    private fun updateLayout(roomViewState: RoomViewState) {
        val isMicEnabled = roomViewState.isMicEnabled
        val isCameraEnabled = roomViewState.isCameraEnabled
        val isLocalMediaEnabled = isMicEnabled && isCameraEnabled
        binding.imgMuteMic.isEnabled = isLocalMediaEnabled
        binding.imgMuteVideoCam.isEnabled = isLocalMediaEnabled
        val micDrawable =
            if (roomViewState.isAudioMuted || !isLocalMediaEnabled) R.drawable.ic_baseline_mic_off_24 else R.drawable.ic_baseline_mic_24
        val videoDrawable =
            if (roomViewState.isVideoOff || !isLocalMediaEnabled) R.drawable.ic_baseline_videocam_off_24 else R.drawable.ic_baseline_videocam_24
        binding.imgMuteMic.setImageResource(micDrawable)
        binding.imgMuteVideoCam.setImageResource(videoDrawable)

        when (roomViewState.configuration) {
            RoomViewConfiguration.Connecting -> {

            }
            RoomViewConfiguration.Connected -> {
                if (isDisposable) {
                    isDisposable = false
                    disposable.addAll(Observable.interval(1, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { timeStamp ->
                            time = if (timeStamp > 60 * 60) {
                                convertTimeToHHMMSS(timeStamp * 1000)
                            } else {
                                convertTimeToMMSS(timeStamp * 1000)
                            }
                            binding.tvTimer.text = time
                        })
                }


            }
            RoomViewConfiguration.Lobby -> {
            }
        }

    }


    private fun listenerView() {
        with(binding) {
            imgEndVideoCall.setOnClickListener {
                onBackPressed()
            }
            imgMuteMic.setOnClickListener {
                toggleLocalAudio()
            }
            imgMuteVideoCam.setOnClickListener {
                toggleLocalVideo()
            }
            imgSwitchCamera.setOnClickListener {
                roomViewModel.processInput(SwitchCamera)
            }
            imgMessage.setOnClickListener {
//                pictureInPictureMode()
            }

            primaryVideoView.setOnClickListener {
                isHide = !isHide
                if (isHide) {
                    slideUp(llActionClick)

                    clWaiting.isVisible = true
                    imgMessage.isVisible = true
                } else {
                    slideDown(llActionClick)

                    clWaiting.isVisible = false
                    imgMessage.isVisible = false

                }
            }

        }


    }

    private fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(
            0F,  // fromXDelta
            0F,  // toXDelta
            view.height.toFloat(),  // fromYDelta
            0F // toYDelta
        )
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)
    }

    private fun slideDown(view: View) {
        val animate = TranslateAnimation(
            0F,  // fromXDelta
            0F,  // toXDelta
            0F,  // fromYDelta
            view.height.toFloat() * 2 // toYDelta
        )
        animate.duration = 500
        animate.fillAfter = true
        view.startAnimation(animate)
        view.visibility = View.GONE
    }

    private fun pictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(10, 16)
            val pictureInPictureParams = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setActions(
                    listOf(
                        createRemoteAction(
                            R.drawable.ic_baseline_videocam_24,
                            R.string.app_name,
                            10,
                            CONTROL_TYPE_VIDEO
                        ),
                        createRemoteAction(
                            R.drawable.ic_round_call_end_24,
                            R.string.app_name,
                            11,
                            CONTROL_TYPE_END
                        ),
                        createRemoteAction(
                            R.drawable.ic_baseline_mic_24,
                            R.string.app_name,
                            12,
                            CONTROL_TYPE_MIC
                        )
                    )
                )
                .setSourceRectHint(Rect())
                .build()
            enterPictureInPictureMode(pictureInPictureParams)
        } else {
            Log.d("TAG123456", "PIP no support")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pictureInPictureMode()
        } else {
            Log.d("TAG123456", "onUserLeaveHint: Already in PIP")
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            with(binding) {
                imgMessage.isVisible = false
                clWaiting.isVisible = false
                llActionClick.isVisible = false

                val params = ConstraintLayout.LayoutParams(160, 220)
                params.topToTop = 0
                params.bottomToBottom = 0
                params.startToStart = 0
                params.endToEnd = 0
                params.horizontalBias = 0.9f
                params.verticalBias = 0.15f
                cardViewThumbView.layoutParams = params

                cardViewPrimary.radius = 10f

            }
        } else {
            with(binding) {
                imgMessage.isVisible = true
                clWaiting.isVisible = true
                llActionClick.isVisible = true

                val params = ConstraintLayout.LayoutParams(400, 600)
                params.topToTop = 0
                params.bottomToBottom = 0
                params.startToStart = 0
                params.endToEnd = 0
                params.horizontalBias = 0.9f
                params.verticalBias = 0.15f
                cardViewThumbView.layoutParams = params

                cardViewPrimary.radius = 0f


            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteAction(
        @DrawableRes iconResId: Int,
        @StringRes titleResId: Int,
        requestCode: Int,
        controlType: Int
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(this, iconResId),
            getString(titleResId),
            getString(titleResId),
            PendingIntent.getBroadcast(
                this,
                requestCode,
                Intent(ACTION_CONTROL)
                    .putExtra(EXTRA_CONTROL_TYPE, controlType),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    /**
     * A [BroadcastReceiver] for handling action items on the picture-in-picture mode.
     */
    private val broadcastReceiver = object : BroadcastReceiver() {

        // Called when an item is clicked.
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action != ACTION_CONTROL) {
                return
            }

            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                CONTROL_TYPE_END -> {
                    moveTaskToBack(false)
                    onBackPressed()

                }
                CONTROL_TYPE_VIDEO -> toggleLocalVideo()
                CONTROL_TYPE_MIC -> toggleLocalAudio()
            }
        }
    }

    private fun connectToRoom() {
        val token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTS2E3MzE1ODZiMmZiZmY5MDc2YzRlZWQ1MzU1YTM3OThkLTE2NTg0ODM4NTMiLCJncmFudHMiOnsiaWRlbnRpdHkiOiJUSEFOSFRSQU5ESU5IIiwidmlkZW8iOnsicm9vbSI6ImFiY0AxMjMifSwiY2hhdCI6eyJzZXJ2aWNlX3NpZCI6IklTMmFhZDBhNDhlMWNkNDliNmIwOTBiYTgyZTRhNGZhZTgifX0sImlhdCI6MTY1ODQ4Mzg1MywiZXhwIjoxNjU4NDk4MjUzLCJpc3MiOiJTS2E3MzE1ODZiMmZiZmY5MDc2YzRlZWQ1MzU1YTM3OThkIiwic3ViIjoiQUMwMzA3YTg2NzVmN2I0NDk5NzI3ZDU0NzhhMjY1MWZmYSJ9.pTg5Ehv5TAJvBQqljrKUIgvMHKy7S7VM95f0u3Jdbkk"
        val roomName = ""
        val viewEvent = Connect(token, roomName)
        roomViewModel.processInput(viewEvent)
    }

    override fun onResume() {
        super.onResume()
        roomViewModel.processInput(OnResume)
    }

    override fun onPause() {
        super.onPause()
        roomViewModel.processInput(OnPause)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        roomViewModel.processInput(Disconnect)
    }

    private fun toggleLocalVideo() {
        roomViewModel.processInput(ToggleLocalVideo)
    }

    private fun toggleLocalAudio() {
        roomViewModel.processInput(ToggleLocalAudio)
    }

    private fun bindRoomViewEffects(roomViewEffect: RoomViewEffect) {
        when (roomViewEffect) {
            is Connected -> {
                toggleAudioDevice(true)
            }
            Disconnected -> {
                toggleAudioDevice(false)
            }
            PermissionsDenied -> requestPermissions()
            else -> {

            }
        }
    }


    private fun requestPermissions() {
        // nested if statements used to keep lint happy and avoid needing a @SuppressLint decoration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    PERMISSIONS_REQUEST_CODE
                )
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                    ),
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    private fun toggleAudioDevice(enableAudioDevice: Boolean) {
        setVolumeControl(enableAudioDevice)
        val viewEvent = if (enableAudioDevice) ActivateAudioDevice else DeactivateAudioDevice
        roomViewModel.processInput(viewEvent)
    }

    private fun setVolumeControl(setVolumeControl: Boolean) {
        volumeControlStream = if (setVolumeControl) {
            /*
             * Enable changing the volume using the up/down keys during a conversation
             */
            AudioManager.STREAM_VOICE_CALL
        } else {
            savedVolumeControlStream
        }
    }

    private fun renderPrimaryView(primaryParticipant: ParticipantViewState) {
        primaryParticipant.run {
            primaryParticipantController.renderAsPrimary(
                if (isLocalParticipant) getString(R.string.you) else identity,
                screenTrack,
                videoTrack,
                isMuted,
                isMirrored
            )
        }
    }

    private fun renderThumbnails(roomViewState: RoomViewState) {
        val newThumbnails = if (roomViewState.configuration is RoomViewConfiguration.Connected)
            roomViewState.participantThumbnails else null
        newThumbnails?.let {
            if (it.isNotEmpty()) {
                when (it.size) {
                    1 -> {
                        with(binding) {
                            roomGroup.root.isVisible = false
                            recyclerViewParticipant.isVisible = false
                            cardViewThumbView.isVisible = false
                            clViewPrimary.isVisible = true

                        }

                        val participantViewState = it[0]
                        if (participantViewState.isLocalParticipant && isEndCall != 1) {
                            disposable.dispose()
                            onBackPressed()
                        }
                        if (participantViewState.identity.isNullOrEmpty()) {
                            primaryParticipantController.updatePerson(
                                getString(R.string.app_name),
                                ""
                            )
                        }
                    }
                    2 -> {
                        isEndCall = it.size
                        val participantViewState = it[0]
                        with(binding) {
                            roomGroup.root.isVisible = false
                            recyclerViewParticipant.isVisible = false

                            cardViewThumbView.isVisible = true
                            clViewPrimary.isVisible = true

                            setupThumbViewVideoView(participantViewState, thumbViewVideoView)

                            thumbViewVideoView.isVisible = true

                        }
                    }
                    3 -> {
                        isEndCall = it.size
                        val participantViewStateFirst = it[0]
                        val participantViewStateSecond = it[1]
                        val participantViewStateThird = it[2]
                        with(binding) {
                            clViewPrimary.isVisible = false
                            cardViewThumbView.isVisible = false
                            recyclerViewParticipant.isVisible = false

                            roomGroup.root.isVisible = true
                            roomGroup.cardViewThumbViewFour.isVisible = false

                            setupThumbViewVideoView(
                                participantViewStateFirst,
                                roomGroup.thumbViewVideoViewFirst
                            )
                            setupThumbViewVideoView(
                                participantViewStateSecond,
                                roomGroup.thumbViewVideoViewSecond
                            )
                            setupThumbViewVideoView(
                                participantViewStateThird,
                                roomGroup.thumbViewVideoViewThird
                            )

                        }

                    }
                    4 -> {

                        isEndCall = it.size
                        val participantViewStateFirst = it[0]
                        val participantViewStateSecond = it[1]
                        val participantViewStateThird = it[2]
                        val participantViewStateFour = it[3]
                        with(binding) {
                            clViewPrimary.isVisible = false
                            cardViewThumbView.isVisible = false
                            recyclerViewParticipant.isVisible = false
                            roomGroup.root.isVisible = true

                            setupThumbViewVideoView(
                                participantViewStateFirst,
                                roomGroup.thumbViewVideoViewFirst
                            )
                            setupThumbViewVideoView(
                                participantViewStateSecond,
                                roomGroup.thumbViewVideoViewSecond
                            )
                            setupThumbViewVideoView(
                                participantViewStateThird,
                                roomGroup.thumbViewVideoViewThird
                            )
                            setupThumbViewVideoView(
                                participantViewStateFour,
                                roomGroup.thumbViewVideoViewFour
                            )

                        }

                    }
                    else -> {
                        isEndCall = it.size
                        with(binding) {
                            roomGroup.root.isVisible = false
                            cardViewThumbView.isVisible = false

                            clViewPrimary.isVisible = true
                            recyclerViewParticipant.isVisible = true

                            participantAdapter.submitList(it.drop(1))

                        }

                    }
                }

            }

        }
    }

    private fun setupThumbViewVideoView(
        participantViewState: ParticipantViewState,
        participantThumbView: ParticipantThumbView
    ) {
        participantThumbView.run {
            val identity = if (participantViewState.isLocalParticipant)
                getString(R.string.you) else participantViewState.identity
            setIdentityy(identity ?: "")
            setMuted(participantViewState.isMuted)
            setPinned(participantViewState.isPinned)

            updateVideoTrackFirst(participantViewState)
        }
    }

    private fun updateVideoTrackFirst(participantViewState: ParticipantViewState) {
        binding.roomGroup.thumbViewVideoViewFirst.run {
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

    private fun updateVideoTrackSecond(participantViewState: ParticipantViewState) {
        binding.roomGroup.thumbViewVideoViewSecond.run {
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

    private fun updateVideoTrackThird(participantViewState: ParticipantViewState) {
        binding.roomGroup.thumbViewVideoViewThird.run {
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

    private fun updateVideoTrackFour(participantViewState: ParticipantViewState) {
        binding.roomGroup.thumbViewVideoViewFour.run {
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