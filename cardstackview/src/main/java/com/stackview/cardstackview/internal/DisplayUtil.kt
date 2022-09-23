package com.stackview.cardstackview.internal

import android.content.Context

object DisplayUtil {
    @JvmStatic
    fun dpToPx(context: Context, dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}