package com.example.twiliovideo.room

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.IntDef
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.twiliovideo.R
import com.twilio.video.VideoScaleType
import com.twilio.video.VideoTextureView
import com.twilio.video.VideoTrack

/**
 * Created by ThanhTran on 6/23/2022.
 */
abstract class ParticipantView : FrameLayout {

    companion object {
        val DEFAULT_VIDEO_SCALE_TYPE = VideoScaleType.ASPECT_FIT
    }


    var identity = ""
    var state = State.NO_VIDEO
    var mirror = false
    var scaleType = VideoScaleType.ASPECT_FIT.ordinal


    var videoTrack: VideoTrack? = null
    lateinit var videoLayout: ConstraintLayout
    lateinit var videoView: VideoTextureView
    lateinit var selectedLayout: ConstraintLayout
    lateinit var stubImage: ImageView
    lateinit var selectedIdentity: TextView
    var audioToggle: AppCompatImageView? = null
    var pinImage: ImageView? = null
    var muteMicPrimary: AppCompatTextView? = null

    constructor(context: Context) : super(context) {
        initParams(context, null)
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        initParams(context, attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        initParams(context, attributeSet)

    }

    constructor(
        context: Context,
        attributeSet: AttributeSet?,
        @AttrRes defStyleAttr: Int,
        @StyleRes defStyleRes: Int
    ) : super(
        context,
        attributeSet,
        defStyleAttr,
        defStyleRes
    ) {
        initParams(context, attributeSet)
    }

    fun setIdentityy(identity: String) {
        this.identity = identity
        selectedIdentity.text = identity
    }

    open fun setStated(state: Int) {
        this.state = state
        when (state) {
            State.SWITCHED_OFF, State.VIDEO -> videoState()
            State.NO_VIDEO, State.SELECTED -> {
                videoLayout.visibility = GONE
                videoView.visibility = GONE
                selectedLayout.visibility = VISIBLE
                stubImage.visibility = VISIBLE
                selectedIdentity.visibility = VISIBLE
            }
            else -> {}
        }
    }

    private fun videoState() {
        selectedLayout.visibility = GONE
        stubImage.visibility = GONE
        selectedIdentity.visibility = GONE
        videoLayout.visibility = VISIBLE
        videoView.visibility = VISIBLE
    }

    open fun setMirrored(mirror: Boolean) {
        this.mirror = mirror
        videoView.mirror = this.mirror
    }

    fun setScaleTypes(scaleType: Int) {
        this.scaleType = scaleType
        videoView.videoScaleType = VideoScaleType.values()[this.scaleType]
    }

    open fun setMuted(muted: Boolean) {
        if (audioToggle != null) {
            audioToggle?.isVisible = muted
        }
    }

    open fun setMuteMicPrimary(muted: Boolean) {
        if (muteMicPrimary != null) {
            muteMicPrimary?.isVisible = muted
            muteMicPrimary?.text = identity
        }
    }

    open fun setPinned(pinned: Boolean) {
        if (pinImage != null) pinImage?.isVisible = pinned
    }

    open fun getVideoTextureView(): VideoTextureView {
        return videoView
    }


    private fun initParams(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val syllables = context.theme
                .obtainStyledAttributes(attrs, R.styleable.ParticipantView, 0, 0)

            // obtain identity
            val identityResId = syllables.getResourceId(R.styleable.ParticipantView_identity, -1)
            identity = if (identityResId != -1) context.getString(identityResId) else ""

            // obtain state
            state = syllables.getInt(
                R.styleable.ParticipantView_state, State.NO_VIDEO
            )

            // obtain mirror
            mirror = syllables.getBoolean(R.styleable.ParticipantView_mirror, false)

            // obtain scale type
            scaleType = syllables.getInt(
                R.styleable.ParticipantView_type, DEFAULT_VIDEO_SCALE_TYPE.ordinal
            )
            syllables.recycle()
        }
    }


    @IntDef(State.VIDEO, State.NO_VIDEO, State.SELECTED, State.SWITCHED_OFF)
    @Retention(AnnotationRetention.SOURCE)
    annotation class State {
        companion object {
            const val VIDEO = 0
            const val NO_VIDEO = 1
            const val SELECTED = 2
            const val SWITCHED_OFF = 3
        }
    }

}