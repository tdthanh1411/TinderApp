package com.stackview.cardstackview.internal

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.stackview.cardstackview.CardStackLayoutManager
import com.stackview.cardstackview.Duration
import com.stackview.cardstackview.SwipeAnimationSetting
import kotlin.math.abs

class CardStackSnapHelper : SnapHelper() {
    private var velocityX = 0
    private var velocityY = 0
    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray {
        if (layoutManager is CardStackLayoutManager) {
            if (layoutManager.findViewByPosition(layoutManager.topPosition) != null) {
                val x = targetView.translationX.toInt()
                val y = targetView.translationY.toInt()
                if (x != 0 || y != 0) {
                    val setting = layoutManager.cardStackSetting
                    val horizontal = abs(x) / targetView.width.toFloat()
                    val vertical = abs(y) / targetView.height.toFloat()
                    val duration =
                        Duration.fromVelocity(if (velocityY < velocityX) velocityX else velocityY)
                    if (duration == Duration.Fast || setting.swipeThreshold < horizontal || setting.swipeThreshold < vertical) {
                        val state = layoutManager.cardStackState
                        if (setting.directions.contains(state.direction)) {
                            state.targetPosition = state.topPosition + 1
                            val swipeAnimationSetting = SwipeAnimationSetting.Builder()
                                .setDirection(setting.swipeAnimationSetting.direction)
                                .setDuration(duration.duration)
                                .setInterpolator(setting.swipeAnimationSetting.interpolator)
                                .build()
                            layoutManager.setSwipeAnimationSetting(swipeAnimationSetting)
                            velocityX = 0
                            velocityY = 0
                            val scroller = CardStackSmoothScroller(
                                CardStackSmoothScroller.ScrollType.ManualSwipe,
                                layoutManager
                            )
                            scroller.targetPosition = layoutManager.topPosition
                            layoutManager.startSmoothScroll(scroller)
                        } else {
                            val scroller = CardStackSmoothScroller(
                                CardStackSmoothScroller.ScrollType.ManualCancel,
                                layoutManager
                            )
                            scroller.targetPosition = layoutManager.topPosition
                            layoutManager.startSmoothScroll(scroller)
                        }
                    } else {
                        val scroller = CardStackSmoothScroller(
                            CardStackSmoothScroller.ScrollType.ManualCancel,
                            layoutManager
                        )
                        scroller.targetPosition = layoutManager.topPosition
                        layoutManager.startSmoothScroll(scroller)
                    }
                }
            }
        }
        return IntArray(2)
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        if (layoutManager is CardStackLayoutManager) {
            val view = layoutManager.findViewByPosition(layoutManager.topPosition)
            if (view != null) {
                val x = view.translationX.toInt()
                val y = view.translationY.toInt()
                return if (x == 0 && y == 0) {
                    null
                } else view
            }
        }
        return null
    }

    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager,
        velocityX: Int,
        velocityY: Int
    ): Int {
        this.velocityX = abs(velocityX)
        this.velocityY = abs(velocityY)
        if (layoutManager is CardStackLayoutManager) {
            return layoutManager.topPosition
        }
        return RecyclerView.NO_POSITION
    }
}