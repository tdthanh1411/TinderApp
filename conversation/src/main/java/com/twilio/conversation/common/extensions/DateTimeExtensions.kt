package com.twilio.conversation.common.extensions

import android.content.Context
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import com.twilio.conversation.R
import com.twilio.conversation.common.enums.SendStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.yearsUntil
import java.text.SimpleDateFormat
import java.util.*

fun Long.asTimeString(): String = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(this))

fun Long.asDateString(): String =
    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(this))

fun Long.asMessageCount(): String = if (this > 99) "99+" else this.toString()

fun Long.asMessageDateString(): String {
    if (this == 0L) {
        return ""
    }

    val instant = Instant.fromEpochMilliseconds(this)
    val now = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()
    val days: Int = instant.daysUntil(now, timeZone)

    val dateFormat = if (days == 0) "H:mm" else "dd-MM-yyyy H:mm"
    return SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(this))
}

fun Long.asLastMessageDateString(context: Context): String {
    if (this == 0L) {
        return ""
    }

    val instant = Instant.fromEpochMilliseconds(this)
    val now = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()

    val days: Int = instant.daysUntil(now, timeZone)
    val weeks: Int = instant.weeksUntil(now, timeZone)
    val years: Int = instant.yearsUntil(now, timeZone)

    return when {
        years > 0 -> context.resources.getQuantityString(R.plurals.years_ago, years, years)
        weeks > 0 -> getConversationTimestamp(this, context)
        days >= 1 -> getConversationTimestamp(this, context)
        else -> asTimeString() // today
    }
}



private fun getFormatter(pattern: String, context: Context): SimpleDateFormat {
    var formattedPattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern)

    if (DateFormat.is24HourFormat(context)) {
        formattedPattern = formattedPattern
            .replace("h", "HH")
            .replace("K", "HH")
            .replace(" a".toRegex(), "")
    }

    return SimpleDateFormat(formattedPattern, Locale.getDefault())
}

fun getConversationTimestamp(date: Long, context: Context): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance()
    then.timeInMillis = date

    return when {
        now.isSameDay(then) -> getFormatter("h:mm a", context)
        now.isSameWeek(then) -> getFormatter("E", context)
        now.isSameYear(then) -> getFormatter("MMM d", context)
        else -> getFormatter("MM/d/yy", context)
    }.format(date)
}

fun SendStatus.asLastMessageStatusIcon() = when (this) {
    SendStatus.SENDING -> R.drawable.ic_waiting_message
    SendStatus.SENT -> R.drawable.ic_sent_message
    SendStatus.ERROR -> R.drawable.ic_failed_message
    else -> 0
}

fun SendStatus.asLastMessageTextColor(context: Context) = when (this) {
    SendStatus.ERROR -> ContextCompat.getColor(context, R.color.red)
    else -> ContextCompat.getColor(context, R.color.color_text_subtitle)
}