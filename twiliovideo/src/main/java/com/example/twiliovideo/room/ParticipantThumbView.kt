package com.example.twiliovideo.room

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.example.twiliovideo.R
import com.example.twiliovideo.databinding.ParticipantViewBinding

/**
 * Created by ThanhTran on 6/23/2022.
 */
class ParticipantThumbView : ParticipantView {

    private lateinit var binding: ParticipantViewBinding

    constructor(context: Context) : super(context) {
        init(context)
    }


    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(context)
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(context)
    }

    constructor(
        context: Context,
        attributeSet: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attributeSet,
        defStyleAttr,
        defStyleRes
    ) {
        init(context)
    }

    private fun init(context: Context) {
        binding = ParticipantViewBinding.inflate(LayoutInflater.from(context), this, true)
        videoLayout = binding.videoLayout
        videoView = binding.video
        selectedLayout = binding.selectedLayout
        stubImage = binding.stub
        selectedIdentity = binding.selectedIdentity
        audioToggle = binding.imgMicOffThumbnail

        setIdentityy(identity)
        setStated(state)
        setMirrored(mirror)
        setScaleTypes(scaleType)
    }

    override fun setStated(state: Int) {
        super.setStated(state)
        binding.participantTrackSwitchOffBackground.visibility = isSwitchOffViewVisible(state)

        var resId: Int = R.drawable.participant_background
        if (state == State.SELECTED) {
            resId = R.drawable.participant_selected_background
        }
        selectedLayout.background = ContextCompat.getDrawable(context, resId)
    }

    private fun isSwitchOffViewVisible(state: Int): Int {
        return if (state == State.SWITCHED_OFF) VISIBLE else GONE
    }


}