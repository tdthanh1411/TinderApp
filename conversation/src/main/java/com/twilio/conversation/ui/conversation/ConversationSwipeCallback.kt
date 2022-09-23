package com.twilio.conversation.ui.conversation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversation.R
import com.twilio.conversation.adapter.ConversationAdapter
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.conversation.Deleted
import java.lang.Float.max
import java.lang.Float.min

class ConversationSwipeCallback(
    val context: Context,
    private val adapter: ConversationAdapter,
    val credentialStorage: CredentialStorage
) : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {


    var onDelete: (String, List<Deleted>, Long,String) -> Unit = { _, _, _,_-> }

    var onMute: (String) -> Unit = {}

    var onUnMute: (String) -> Unit = {}

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_round_delete_24)!!

    private val muteIcon =
        ContextCompat.getDrawable(context, R.drawable.ic_round_notifications_off_24)!!

    private val unMuteIcon =
        ContextCompat.getDrawable(context, R.drawable.ic_round_notifications_active_24)!!

    private val deleteBackground = ContextCompat.getDrawable(context, R.color.red)!!

    private val muteBackground = ContextCompat.getDrawable(context, R.color.blue)!!

    private val swipeLimit = context.resources.getDimension(R.dimen.dimen_100)

    private var swipeBack = false

    private var onSwipeBackAction = {}

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    @SuppressLint("ClickableViewAccessibility")
    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack =
                event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            false
        }


        val position = viewHolder.bindingAdapterPosition
        val conversation = adapter.conversations[position]
        val conversationSid = conversation.sid
        val deleted = conversation.deleted
        val messageIndex = conversation.messageIndex
        val avatarUrl = conversation.avatarUrl

        onSwipeBackAction = when {
            dX > swipeLimit && adapter.isMuted(position) -> {
                { onUnMute(conversationSid) }
            }

            dX > swipeLimit && !adapter.isMuted(position) -> {
                { onMute(conversationSid) }
            }

            dX < -swipeLimit -> {
                { onDelete(conversationSid, deleted, messageIndex,avatarUrl) }
            }

            else -> {
                {}
            }
        }

        val limitedDX = max(-swipeLimit, min(dX, swipeLimit))
        super.onChildDraw(
            canvas,
            recyclerView,
            viewHolder,
            limitedDX,
            dY,
            actionState,
            isCurrentlyActive
        )

        val itemView = viewHolder.itemView

        if (limitedDX > 0) { // Swiping to the right - mute/unmute conversation
            drawMuteIcon(canvas, itemView, limitedDX, position)
        } else if (limitedDX < 0) { // Swiping to the left - delete conversation
            drawDelete(canvas, itemView, limitedDX)
        }
    }

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        if (swipeBack) {
            swipeBack = false
            onSwipeBackAction()
            return 0
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    private fun drawMuteIcon(canvas: Canvas, itemView: View, dX: Float, position: Int) {
        val isMuted = adapter.isMuted(position)
        val icon = if (isMuted) unMuteIcon else muteIcon
        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
        val iconBottom = iconTop + icon.intrinsicHeight
        val iconLeft = itemView.left + iconMargin
        val iconRight = iconLeft + icon.intrinsicWidth

        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        muteBackground.setBounds(
            itemView.left,
            itemView.top,
            itemView.left + dX.toInt(),
            itemView.bottom
        )
        muteBackground.draw(canvas)
        icon.draw(canvas)
    }

    private fun drawDelete(canvas: Canvas, itemView: View, dX: Float) {
        val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
        val iconBottom = iconTop + deleteIcon.intrinsicHeight
        val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
        val iconRight = itemView.right - iconMargin

        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        deleteBackground.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        deleteBackground.draw(canvas)
        deleteIcon.draw(canvas)
    }
}