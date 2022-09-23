package com.stackview.cardstackview.internal

import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import com.stackview.cardstackview.*

class CardStackSetting {
    @JvmField
    var stackFrom = StackFrom.None
    @JvmField
    var visibleCount = 3
    @JvmField
    var translationInterval = 8.0f
    @JvmField
    var scaleInterval = 0.95f // 0.0f - 1.0f
    @JvmField
    var swipeThreshold = 0.3f // 0.0f - 1.0f
    @JvmField
    var maxDegree = 20.0f
    @JvmField
    var directions = Direction.HORIZONTAL
    @JvmField
    var canScrollHorizontal = true
    @JvmField
    var canScrollVertical = true
    @JvmField
    var swipeableMethod = SwipeableMethod.AutomaticAndManual
    @JvmField
    var swipeAnimationSetting = SwipeAnimationSetting.Builder().build()
    @JvmField
    var rewindAnimationSetting = RewindAnimationSetting.Builder().build()
    @JvmField
    var overlayInterpolator: Interpolator = LinearInterpolator()
}