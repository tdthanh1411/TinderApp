package com.stackview.cardstackview.internal

import android.view.animation.Interpolator
import com.stackview.cardstackview.Direction

interface AnimationSetting {
    val direction: Direction
    val duration: Int
    val interpolator: Interpolator
}