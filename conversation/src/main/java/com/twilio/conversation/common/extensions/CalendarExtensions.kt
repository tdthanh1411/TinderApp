package com.twilio.conversation.common.extensions

import java.util.*

/**
 * Created by ThanhTran on 7/11/2022.
 */
fun Calendar.isSameDay(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(
        Calendar.DAY_OF_YEAR
    )
}

fun Calendar.isSameWeek(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.WEEK_OF_YEAR) == other.get(
        Calendar.WEEK_OF_YEAR
    )
}

fun Calendar.isSameYear(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR)
}
