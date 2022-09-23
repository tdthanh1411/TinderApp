/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.twilio.conversation.ui.message


import android.util.Log
import android.view.View
import com.twilio.conversation.R
import com.twilio.conversation.data.model.message.MessageListViewItem
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object BubbleUtils {

    const val TIMESTAMP_THRESHOLD = 1000

    fun canGroup(message: MessageListViewItem, other: MessageListViewItem?): Boolean {
        if (other == null) return false
        val diff = TimeUnit.MILLISECONDS.toMinutes(abs(message.dateCreated - other.dateCreated))
        return message.compareSender(other) && diff < TIMESTAMP_THRESHOLD
    }

    fun getBubble(canGroupWithPrevious: Boolean, canGroupWithNext: Boolean, isMe: Boolean): Int {
        return when {
            !canGroupWithPrevious && canGroupWithNext -> {
                if (isMe) R.drawable.message_out_first else R.drawable.message_in_first
            }
            canGroupWithPrevious && canGroupWithNext -> {
                if (isMe) R.drawable.message_out_middle else R.drawable.message_in_middle
            }
            canGroupWithPrevious && !canGroupWithNext -> {
                if (isMe) R.drawable.message_out_last else R.drawable.message_in_last
            }
            else -> {
                if (isMe) R.drawable.message_only_out else R.drawable.message_only
            }
        }
    }


    fun getVisibleAvatar(
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ): Int {
        return when {
            !canGroupWithPrevious && canGroupWithNext -> {
                View.INVISIBLE
            }
            canGroupWithPrevious && canGroupWithNext -> {
                View.INVISIBLE
            }
            canGroupWithPrevious && !canGroupWithNext -> {
                View.VISIBLE
            }
            else -> {
                View.VISIBLE
            }
        }
    }

    fun getVisibleDateTime(
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    ): Boolean {
        return when {
            !canGroupWithPrevious && !canGroupWithNext -> {
                true
            }
            else -> {
                false
            }
        }
    }

}