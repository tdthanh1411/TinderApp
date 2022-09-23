package com.example.demovideocall.common.extensions

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator

fun View.pulse() {
    val animatorSet = AnimatorSet()
    val object1 = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.95f, 1f)
    val object2 = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.95f, 1f)
    animatorSet.playTogether(object1, object2)
    animatorSet.duration = 150
    animatorSet.interpolator = AccelerateInterpolator()
    animatorSet.start()
}

fun View.pulseOnlyUp() {
    val animatorSet = AnimatorSet()
    val object1 = ObjectAnimator.ofFloat(this, "scaleY", 0.95f, 1f, 1f)
    val object2 = ObjectAnimator.ofFloat(this, "scaleX", 0.95f, 1f, 1f)
    animatorSet.playTogether(object1, object2)
    animatorSet.duration = 250
    animatorSet.interpolator = AccelerateInterpolator()
    animatorSet.start()
}

fun View.scale(f: Float) {
    val animatorSet = AnimatorSet()
    val object1 = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1f + f, 1f + f)
    val object2 = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1f + f, 1f + f)
    animatorSet.playTogether(object1, object2)
    animatorSet.duration = 250
    animatorSet.interpolator = AccelerateInterpolator()
    animatorSet.start()
}