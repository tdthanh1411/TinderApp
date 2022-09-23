package com.stackview.cardstackview.internal

import androidx.recyclerview.widget.RecyclerView
import com.stackview.cardstackview.Direction
import kotlin.math.abs
import kotlin.math.min

class CardStackState {
    @JvmField
    var status = Status.Idle
    @JvmField
    var width = 0
    @JvmField
    var height = 0
    @JvmField
    var dx = 0
    @JvmField
    var dy = 0
    @JvmField
    var topPosition = 0
    @JvmField
    var targetPosition = RecyclerView.NO_POSITION
    @JvmField
    var proportion = 0.0f

    enum class Status {
        Idle, Dragging, RewindAnimating, AutomaticSwipeAnimating, AutomaticSwipeAnimated, ManualSwipeAnimating, ManualSwipeAnimated;

        val isBusy: Boolean
            get() = this != Idle
        val isDragging: Boolean
            get() = this == Dragging
        val isSwipeAnimating: Boolean
            get() = this == ManualSwipeAnimating || this == AutomaticSwipeAnimating

        fun toAnimatedStatus(): Status {
            return when (this) {
                ManualSwipeAnimating -> ManualSwipeAnimated
                AutomaticSwipeAnimating -> AutomaticSwipeAnimated
                else -> Idle
            }
        }
    }

    fun next(state: Status) {
        status = state
    }

    val direction: Direction
        get() = if (abs(dy) < abs(dx)) {
            if (dx < 0.0f) {
                Direction.Left
            } else {
                Direction.Right
            }
        } else {
            if (dy < 0.0f) {
                Direction.Top
            } else {
                Direction.Bottom
            }
        }
    val ratio: Float
        get() {
            val absDx = abs(dx)
            val absDy = abs(dy)
            val ratio = if (absDx < absDy) {
                absDy / (height / 2.0f)
            } else {
                absDx / (width / 2.0f)
            }
            return min(ratio, 1.0f)
        }
    val isSwipeCompleted: Boolean
        get() {
            if (status.isSwipeAnimating) {
                if (topPosition < targetPosition) {
                    if (width < abs(dx) || height < abs(dy)) {
                        return true
                    }
                }
            }
            return false
        }

    fun canScrollToPosition(position: Int, itemCount: Int): Boolean {
        if (position == topPosition) {
            return false
        }
        if (position < 0) {
            return false
        }
        if (itemCount < position) {
            return false
        }
        return !status.isBusy
    }
}