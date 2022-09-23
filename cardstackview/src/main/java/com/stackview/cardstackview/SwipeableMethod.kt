package com.stackview.cardstackview

enum class SwipeableMethod {
    AutomaticAndManual, Automatic, Manual, None;

    fun canSwipe(): Boolean {
        return canSwipeAutomatically() || canSwipeManually()
    }

    fun canSwipeAutomatically(): Boolean {
        return this == AutomaticAndManual || this == Automatic
    }

    fun canSwipeManually(): Boolean {
        return this == AutomaticAndManual || this == Manual
    }
}