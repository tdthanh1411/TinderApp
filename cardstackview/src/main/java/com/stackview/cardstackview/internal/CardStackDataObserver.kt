package com.stackview.cardstackview.internal

import androidx.recyclerview.widget.RecyclerView
import com.stackview.cardstackview.CardStackLayoutManager
import java.lang.IllegalStateException

class CardStackDataObserver(private val recyclerView: RecyclerView) :
    RecyclerView.AdapterDataObserver() {
    override fun onChanged() {
        val manager = cardStackLayoutManager
        manager.topPosition = 0
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        // Do nothing
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        // Do nothing
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        // Do nothing
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        // Top Position may need to be adjusted if the element is removed
        // Adjustment is required when all elements are deleted and when elements before TopPosition are deleted.
        val manager = cardStackLayoutManager
        val topPosition = manager.topPosition
        if (manager.itemCount == 0) {
            // When all elements are deleted
            manager.topPosition = 0
        } else if (positionStart < topPosition) {
            // When elements before TopPosition are deleted
            val diff = topPosition - positionStart
            manager.topPosition = Math.min(topPosition - diff, manager.itemCount - 1)
        }
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        val manager = cardStackLayoutManager
        manager.removeAllViews()
    }

    private val cardStackLayoutManager: CardStackLayoutManager
        private get() {
            val manager = recyclerView.layoutManager
            if (manager is CardStackLayoutManager) {
                return manager
            }
            throw IllegalStateException("CardStackView must be set CardStackLayoutManager.")
        }
}