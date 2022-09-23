package com.example.demovideocall.ui.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversations.NotificationPayload
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by ThanhTran on 7/25/2022.
 */
class FCMService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun launch(block: suspend CoroutineScope.() -> Unit) = serviceScope.launch(
        context = CoroutineExceptionHandler { _, e ->
            Timber.e(
                e,
                "Coroutine failed ${e.localizedMessage}"
            )
        },
        block = block
    )

    private var fcmManagerImpl: FCMManagerImpl? = null

    override fun onCreate() {
        super.onCreate()
        if (fcmManagerImpl == null) {
            fcmManagerImpl = FCMManagerImpl(
                applicationContext,
                ConversationsClient.INSTANCE,
                CredentialStorage(applicationContext)
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        launch {
            fcmManagerImpl?.onNewToken(token)
        }
    }


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)


        launch {
            if (message.data.isNotEmpty()) {
                fcmManagerImpl?.onMessageReceived(NotificationPayload(message.data))
            }
        }
    }

}