package com.example.demovideocall.ui.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.*
import com.example.demovideocall.R
import com.example.demovideocall.ui.MainActivity
import com.example.demovideocall.ui.message.MessageActivity
import com.twilio.conversation.common.extensions.ConversationsException
import com.twilio.conversation.common.extensions.registerFCMToken
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversations.NotificationPayload
import timber.log.Timber

/**
 * Created by ThanhTran on 7/25/2022.
 */

private const val NOTIFICATION_CONVERSATION_ID = "twilio_notification_chat_id"
private const val NOTIFICATION_NAME = "Twilio Notification Chat"
private const val NOTIFICATION_ID = 12345

interface FCMManager : LifecycleObserver {
    suspend fun onNewToken(token: String)
    suspend fun onMessageReceived(payload: NotificationPayload)
    fun showNotification(payload: NotificationPayload)
}

class FCMManagerImpl(
    private val context: Context,
    private val conversationsClient: ConversationsClient,
    private val credentialStorage: CredentialStorage
) : FCMManager {
    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private var isBackgrounded = true
    var lifecycleEventObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            isBackgrounded = false
            notificationManager.cancel(NOTIFICATION_ID)
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            isBackgrounded = true
        }
    }


    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
    }

    override suspend fun onNewToken(token: String) {
        try {
            if (token != credentialStorage.fcmToken && conversationsClient.isClientCreated) {
                conversationsClient.getConversationsClient().registerFCMToken(token)
            }
            credentialStorage.fcmToken = token
        } catch (e: ConversationsException) {
            Timber.d("Failed to onNewToken: ${e.error}")
        }
    }

    override suspend fun onMessageReceived(payload: NotificationPayload) {

        if (conversationsClient.isClientCreated) {
            conversationsClient.getConversationsClient().handleNotification(payload)
        }
        // Ignore everything we don't support
        if (payload.type == NotificationPayload.Type.UNKNOWN) return

        if (isBackgrounded) {
            showNotification(payload)
        }
    }

    private fun getTargetIntent(
        type: NotificationPayload.Type,
        conversationSid: String,
        conversationTitle: String
    ): Intent {
        return when (type) {
            NotificationPayload.Type.NEW_MESSAGE -> MessageActivity.getStartIntent(
                context,
                conversationSid,
                conversationTitle,
                0L,
                2,
            arrayListOf()
            )
            NotificationPayload.Type.ADDED_TO_CONVERSATION -> MessageActivity.getStartIntent(
                context,
                conversationSid,
                conversationTitle,
                0L,
                2,
                arrayListOf()
            )
            NotificationPayload.Type.REMOVED_FROM_CONVERSATION -> MainActivity.getStartIntent(
                context
            )
            else -> MainActivity.getStartIntent(context)
        }
    }


    val NotificationPayload.textForNotification: String
        get() = when (type) {
            NotificationPayload.Type.NEW_MESSAGE -> when {
                mediaCount > 1 -> context.getString(R.string.notification_media_message, mediaCount)
                mediaCount > 0 -> context.getString(R.string.notification_media) + ": " +
                        mediaFilename.ifEmpty { Formatter.formatShortFileSize(context, mediaSize) }
                else -> body
            }
            else -> body
        }

    val NotificationPayload.largeIconId: Int?
        get() = when (type) {
            NotificationPayload.Type.NEW_MESSAGE -> when {
                mediaCount > 1 -> R.drawable.ic_media_multiple_attachments
                mediaCount > 0 -> with(mediaContentType) {
                    when {
                        startsWith("image/") || startsWith("jpeg") -> R.drawable.ic_media_image
                        startsWith("video/") -> R.drawable.ic_media_video
                        startsWith("audio/") -> R.drawable.ic_media_audio
                        else -> R.drawable.ic_media_document
                    }
                }
                else -> null
            }
            else -> null
        }

    val NotificationPayload.largeIcon
        get() = largeIconId?.let {
            ContextCompat.getDrawable(
                context,
                it
            )?.toBitmap()
        }


    private fun buildNotification(payload: NotificationPayload): Notification {
        val intent =
            getTargetIntent(payload.type, payload.conversationSid, payload.conversationTitle)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }


        val title = when (payload.type) {
            NotificationPayload.Type.NEW_MESSAGE -> context.getString(R.string.notification_new_message)
            NotificationPayload.Type.ADDED_TO_CONVERSATION -> context.getString(R.string.notification_added_to_conversation)
            NotificationPayload.Type.REMOVED_FROM_CONVERSATION -> context.getString(R.string.notification_removed_from_conversation)
            else -> context.getString(R.string.notification_generic)
        }

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CONVERSATION_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(payload.largeIcon)
            .setContentTitle(title)
            .setContentText(payload.textForNotification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setColor(Color.rgb(33, 150, 243))

        val soundFileName = payload.sound
        if (context.resources.getIdentifier(soundFileName, "raw", context.packageName) != 0) {
            val sound = Uri.parse("android.resource://${context.packageName}/raw/$soundFileName")
            notificationBuilder.setSound(sound)
            Timber.d("Playing specified sound $soundFileName")
        } else {
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND)
            Timber.d("Playing default sound")
        }

        return notificationBuilder.build()
    }

    override fun showNotification(payload: NotificationPayload) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CONVERSATION_ID,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = buildNotification(payload)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }


}