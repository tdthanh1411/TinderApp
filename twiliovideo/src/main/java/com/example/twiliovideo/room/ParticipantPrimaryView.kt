package com.example.twiliovideo.room

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.example.twiliovideo.databinding.ParticipantPrimaryViewBinding

/**
 * Created by ThanhTran on 6/23/2022.
 */

class ParticipantPrimaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ParticipantView(context, attrs, defStyleAttr) {

    private val binding: ParticipantPrimaryViewBinding =
        ParticipantPrimaryViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        videoLayout = binding.videoLayout
        videoView = binding.video
        selectedLayout = binding.clSelected
        stubImage = binding.imgStub
        selectedIdentity = binding.selectedIdentity
        muteMicPrimary = binding.tvUserMicOff
        setIdentityy(identity)
        setStated(state)
        setMirrored(mirror)
        setScaleTypes(scaleType)
    }

    fun showIdentityBadge(show: Boolean) {
    }
}