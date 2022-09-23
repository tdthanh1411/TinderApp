package com.stackview.cardstackview

import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.recyclerview.widget.RecyclerView
import com.stackview.cardstackview.internal.CardStackSetting
import com.stackview.cardstackview.internal.CardStackSmoothScroller
import com.stackview.cardstackview.internal.CardStackState
import com.stackview.cardstackview.internal.DisplayUtil.dpToPx

class CardStackLayoutManager @JvmOverloads constructor(
    private val context: Context,
    listener: CardStackListener = CardStackListener.DEFAULT
) : RecyclerView.LayoutManager(), RecyclerView.SmoothScroller.ScrollVectorProvider {
    var cardStackListener = CardStackListener.DEFAULT
    val cardStackSetting = CardStackSetting()
    val cardStackState = CardStackState()
    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, s: RecyclerView.State) {
        update(recycler)
        if (s.didStructureChange()) {
            val topView = topView
            if (topView != null) {
                cardStackListener.onCardAppeared(topView, cardStackState.topPosition)
            }
        }
    }

    override fun canScrollHorizontally(): Boolean {
        return cardStackSetting.swipeableMethod.canSwipe() && cardStackSetting.canScrollHorizontal
    }

    override fun canScrollVertically(): Boolean {
        return cardStackSetting.swipeableMethod.canSwipe() && cardStackSetting.canScrollVertical
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        s: RecyclerView.State
    ): Int {
        if (cardStackState.topPosition == itemCount) {
            return 0
        }
        when (cardStackState.status) {
            CardStackState.Status.Idle -> if (cardStackSetting.swipeableMethod.canSwipeManually()) {
                cardStackState.dx -= dx
                update(recycler)
                return dx
            }
            CardStackState.Status.Dragging -> if (cardStackSetting.swipeableMethod.canSwipeManually()) {
                cardStackState.dx -= dx
                update(recycler)
                return dx
            }
            CardStackState.Status.RewindAnimating -> {
                cardStackState.dx -= dx
                update(recycler)
                return dx
            }
            CardStackState.Status.AutomaticSwipeAnimating -> if (cardStackSetting.swipeableMethod.canSwipeAutomatically()) {
                cardStackState.dx -= dx
                update(recycler)
                return dx
            }
            CardStackState.Status.AutomaticSwipeAnimated -> {}
            CardStackState.Status.ManualSwipeAnimating -> if (cardStackSetting.swipeableMethod.canSwipeManually()) {
                cardStackState.dx -= dx
                update(recycler)
                return dx
            }
            CardStackState.Status.ManualSwipeAnimated -> {}
        }
        return 0
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        s: RecyclerView.State
    ): Int {
        if (cardStackState.topPosition == itemCount) {
            return 0
        }
        when (cardStackState.status) {
            CardStackState.Status.Idle -> if (cardStackSetting.swipeableMethod.canSwipeManually()) {
                cardStackState.dy -= dy
                update(recycler)
                return dy
            }
            CardStackState.Status.Dragging -> if (cardStackSetting.swipeableMethod.canSwipeManually()) {
                cardStackState.dy -= dy
                update(recycler)
                return dy
            }
            CardStackState.Status.RewindAnimating -> {
                cardStackState.dy -= dy
                update(recycler)
                return dy
            }
            CardStackState.Status.AutomaticSwipeAnimating -> if (cardStackSetting.swipeableMethod.canSwipeAutomatically()) {
                cardStackState.dy -= dy
                update(recycler)
                return dy
            }
            CardStackState.Status.AutomaticSwipeAnimated -> {}
            CardStackState.Status.ManualSwipeAnimating -> if (cardStackSetting.swipeableMethod.canSwipeManually()) {
                cardStackState.dy -= dy
                update(recycler)
                return dy
            }
            CardStackState.Status.ManualSwipeAnimated -> {}
        }
        return 0
    }

    override fun onScrollStateChanged(s: Int) {
        when (s) {
            RecyclerView.SCROLL_STATE_IDLE -> if (cardStackState.targetPosition == RecyclerView.NO_POSITION) {
                cardStackState.next(CardStackState.Status.Idle)
                cardStackState.targetPosition = RecyclerView.NO_POSITION
            } else if (cardStackState.topPosition == cardStackState.targetPosition) {
                cardStackState.next(CardStackState.Status.Idle)
                cardStackState.targetPosition = RecyclerView.NO_POSITION
            } else {
                if (cardStackState.topPosition < cardStackState.targetPosition) {
                    smoothScrollToNext(cardStackState.targetPosition)
                } else {
                    smoothScrollToPrevious(cardStackState.targetPosition)
                }
            }
            RecyclerView.SCROLL_STATE_DRAGGING -> if (cardStackSetting.swipeableMethod.canSwipeManually()) {
                cardStackState.next(CardStackState.Status.Dragging)
            }
            RecyclerView.SCROLL_STATE_SETTLING -> {}
        }
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        return null
    }

    override fun scrollToPosition(position: Int) {
        if (cardStackSetting.swipeableMethod.canSwipeAutomatically()) {
            if (cardStackState.canScrollToPosition(position, itemCount)) {
                cardStackState.topPosition = position
                requestLayout()
            }
        }
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        s: RecyclerView.State,
        position: Int
    ) {
        if (cardStackSetting.swipeableMethod.canSwipeAutomatically()) {
            if (cardStackState.canScrollToPosition(position, itemCount)) {
                smoothScrollToPosition(position)
            }
        }
    }

    fun updateProportion(x: Float, y: Float) {
        if (topPosition < itemCount) {
            val view = findViewByPosition(topPosition)
            if (view != null) {
                val half = height / 2.0f
                cardStackState.proportion = -(y - half - view.top) / half
            }
        }
    }

    private fun update(recycler: RecyclerView.Recycler) {
        cardStackState.width = width
        cardStackState.height = height
        if (cardStackState.isSwipeCompleted) {
            removeAndRecycleView(topView!!, recycler)
            val direction = cardStackState.direction
            cardStackState.next(cardStackState.status.toAnimatedStatus())
            cardStackState.topPosition++
            cardStackState.dx = 0
            cardStackState.dy = 0
            if (cardStackState.topPosition == cardStackState.targetPosition) {
                cardStackState.targetPosition = RecyclerView.NO_POSITION
            }
            Handler(Looper.getMainLooper()).post {
                cardStackListener.onCardSwiped(direction)
                val topView = topView
                if (topView != null) {
                    cardStackListener.onCardAppeared(topView, cardStackState.topPosition)
                }
            }
        }
        detachAndScrapAttachedViews(recycler)
        val parentTop = paddingTop
        val parentLeft = paddingLeft
        val parentRight = width - paddingLeft
        val parentBottom = height - paddingBottom
        var i = cardStackState.topPosition
        while (i < cardStackState.topPosition + cardStackSetting.visibleCount && i < itemCount) {
            val child = recycler.getViewForPosition(i)
            addView(child, 0)
            measureChildWithMargins(child, 0, 0)
            layoutDecoratedWithMargins(child, parentLeft, parentTop, parentRight, parentBottom)
            resetTranslation(child)
            resetScale(child)
            resetRotation(child)
            resetOverlay(child)
            if (i == cardStackState.topPosition) {
                updateTranslation(child)
                resetScale(child)
                updateRotation(child)
                updateOverlay(child)
            } else {
                val currentIndex = i - cardStackState.topPosition
                updateTranslation(child, currentIndex)
                updateScale(child, currentIndex)
                resetRotation(child)
                resetOverlay(child)
            }
            i++
        }
        if (cardStackState.status.isDragging) {
            cardStackListener.onCardDragging(cardStackState.direction, cardStackState.ratio)
        }
    }

    private fun updateTranslation(view: View) {
        view.translationX = cardStackState.dx.toFloat()
        view.translationY = cardStackState.dy.toFloat()
    }

    private fun updateTranslation(view: View, index: Int) {
        val nextIndex = index - 1
        val translationPx = dpToPx(context, cardStackSetting.translationInterval)
        val currentTranslation = (index * translationPx).toFloat()
        val nextTranslation = (nextIndex * translationPx).toFloat()
        val targetTranslation =
            currentTranslation - (currentTranslation - nextTranslation) * cardStackState.ratio
        when (cardStackSetting.stackFrom) {
            StackFrom.None -> {}
            StackFrom.Top -> view.translationY = -targetTranslation
            StackFrom.TopAndLeft -> {
                view.translationY = -targetTranslation
                view.translationX = -targetTranslation
            }
            StackFrom.TopAndRight -> {
                view.translationY = -targetTranslation
                view.translationX = targetTranslation
            }
            StackFrom.Bottom -> view.translationY = targetTranslation
            StackFrom.BottomAndLeft -> {
                view.translationY = targetTranslation
                view.translationX = -targetTranslation
            }
            StackFrom.BottomAndRight -> {
                view.translationY = targetTranslation
                view.translationX = targetTranslation
            }
            StackFrom.Left -> view.translationX = -targetTranslation
            StackFrom.Right -> view.translationX = targetTranslation
        }
    }

    private fun resetTranslation(view: View) {
        view.translationX = 0.0f
        view.translationY = 0.0f
    }

    private fun updateScale(view: View, index: Int) {
        val nextIndex = index - 1
        val currentScale = 1.0f - index * (1.0f - cardStackSetting.scaleInterval)
        val nextScale = 1.0f - nextIndex * (1.0f - cardStackSetting.scaleInterval)
        val targetScale = currentScale + (nextScale - currentScale) * cardStackState.ratio
        when (cardStackSetting.stackFrom) {
            StackFrom.None -> {
                view.scaleX = targetScale
                view.scaleY = targetScale
            }
            StackFrom.Top -> view.scaleX = targetScale
            StackFrom.TopAndLeft -> view.scaleX = targetScale
            StackFrom.TopAndRight -> view.scaleX = targetScale
            StackFrom.Bottom -> view.scaleX = targetScale
            StackFrom.BottomAndLeft -> view.scaleX = targetScale
            StackFrom.BottomAndRight -> view.scaleX = targetScale
            StackFrom.Left ->
                view.scaleY = targetScale
            StackFrom.Right ->
                view.scaleY = targetScale
        }
    }

    private fun resetScale(view: View) {
        view.scaleX = 1.0f
        view.scaleY = 1.0f
    }

    private fun updateRotation(view: View) {
        val degree =
            cardStackState.dx * cardStackSetting.maxDegree / width * cardStackState.proportion
        view.rotation = degree
    }

    private fun resetRotation(view: View) {
        view.rotation = 0.0f
    }

    private fun updateOverlay(view: View) {
        val leftOverlay = view.findViewById<View>(R.id.left_overlay)
        if (leftOverlay != null) {
            leftOverlay.alpha = 0.0f
        }
        val rightOverlay = view.findViewById<View>(R.id.right_overlay)
        if (rightOverlay != null) {
            rightOverlay.alpha = 0.0f
        }
        val topOverlay = view.findViewById<View>(R.id.top_overlay)
        if (topOverlay != null) {
            topOverlay.alpha = 0.0f
        }
        val bottomOverlay = view.findViewById<View>(R.id.bottom_overlay)
        if (bottomOverlay != null) {
            bottomOverlay.alpha = 0.0f
        }
        val direction = cardStackState.direction
        val alpha = cardStackSetting.overlayInterpolator.getInterpolation(
            cardStackState.ratio
        )
        when (direction) {
            Direction.Left -> if (leftOverlay != null) {
                leftOverlay.alpha = alpha
            }
            Direction.Right -> if (rightOverlay != null) {
                rightOverlay.alpha = alpha
            }
            Direction.Top -> if (topOverlay != null) {
                topOverlay.alpha = alpha
            }
            Direction.Bottom -> if (bottomOverlay != null) {
                bottomOverlay.alpha = alpha
            }
        }
    }

    private fun resetOverlay(view: View) {
        val leftOverlay = view.findViewById<View>(R.id.left_overlay)
        if (leftOverlay != null) {
            leftOverlay.alpha = 0.0f
        }
        val rightOverlay = view.findViewById<View>(R.id.right_overlay)
        if (rightOverlay != null) {
            rightOverlay.alpha = 0.0f
        }
        val topOverlay = view.findViewById<View>(R.id.top_overlay)
        if (topOverlay != null) {
            topOverlay.alpha = 0.0f
        }
        val bottomOverlay = view.findViewById<View>(R.id.bottom_overlay)
        if (bottomOverlay != null) {
            bottomOverlay.alpha = 0.0f
        }
    }

    private fun smoothScrollToPosition(position: Int) {
        if (cardStackState.topPosition < position) {
            smoothScrollToNext(position)
        } else {
            smoothScrollToPrevious(position)
        }
    }

    private fun smoothScrollToNext(position: Int) {
        cardStackState.proportion = 0.0f
        cardStackState.targetPosition = position
        val scroller =
            CardStackSmoothScroller(CardStackSmoothScroller.ScrollType.AutomaticSwipe, this)
        scroller.targetPosition = cardStackState.topPosition
        startSmoothScroll(scroller)
    }

    private fun smoothScrollToPrevious(position: Int) {
        val topView = topView
        if (topView != null) {
            cardStackListener.onCardDisappeared(topView, cardStackState.topPosition)
        }
        cardStackState.proportion = 0.0f
        cardStackState.targetPosition = position
        cardStackState.topPosition--
        val scroller =
            CardStackSmoothScroller(CardStackSmoothScroller.ScrollType.AutomaticRewind, this)
        scroller.targetPosition = cardStackState.topPosition
        startSmoothScroll(scroller)
    }

    val topView get() = findViewByPosition(cardStackState.topPosition)
    var topPosition: Int
        get() = cardStackState.topPosition
        set(topPosition) {
            cardStackState.topPosition = topPosition
        }

    fun setStackFrom(stackFrom: StackFrom) {
        cardStackSetting.stackFrom = stackFrom
    }

    fun setVisibleCount(@IntRange(from = 1) visibleCount: Int) {
        require(visibleCount >= 1) { "VisibleCount must be greater than 0." }
        cardStackSetting.visibleCount = visibleCount
    }

    fun setTranslationInterval(@FloatRange(from = 0.0) translationInterval: Float) {
        require(translationInterval >= 0.0f) { "TranslationInterval must be greater than or equal 0.0f" }
        cardStackSetting.translationInterval = translationInterval
    }

    fun setScaleInterval(@FloatRange(from = 0.0) scaleInterval: Float) {
        require(scaleInterval >= 0.0f) { "ScaleInterval must be greater than or equal 0.0f." }
        cardStackSetting.scaleInterval = scaleInterval
    }

    fun setSwipeThreshold(@FloatRange(from = 0.0, to = 1.0) swipeThreshold: Float) {
        require(!(swipeThreshold < 0.0f || 1.0f < swipeThreshold)) { "SwipeThreshold must be 0.0f to 1.0f." }
        cardStackSetting.swipeThreshold = swipeThreshold
    }

    fun setMaxDegree(@FloatRange(from = (-360.0f).toDouble(), to = 360.0) maxDegree: Float) {
        require(!(maxDegree < -360.0f || 360.0f < maxDegree)) { "MaxDegree must be -360.0f to 360.0f" }
        cardStackSetting.maxDegree = maxDegree
    }

    fun setDirections(directions: List<Direction>) {
        cardStackSetting.directions = directions
    }

    fun setCanScrollHorizontal(canScrollHorizontal: Boolean) {
        cardStackSetting.canScrollHorizontal = canScrollHorizontal
    }

    fun setCanScrollVertical(canScrollVertical: Boolean) {
        cardStackSetting.canScrollVertical = canScrollVertical
    }

    fun setSwipeableMethod(swipeableMethod: SwipeableMethod) {
        cardStackSetting.swipeableMethod = swipeableMethod
    }

    fun setSwipeAnimationSetting(swipeAnimationSetting: SwipeAnimationSetting) {
        cardStackSetting.swipeAnimationSetting = swipeAnimationSetting
    }

    fun setRewindAnimationSetting(rewindAnimationSetting: RewindAnimationSetting) {
        cardStackSetting.rewindAnimationSetting = rewindAnimationSetting
    }

    fun setOverlayInterpolator(overlayInterpolator: Interpolator) {
        cardStackSetting.overlayInterpolator = overlayInterpolator
    }

    init {
        cardStackListener = listener
    }
}