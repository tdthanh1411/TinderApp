package com.example.demovideocall.ui.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.twilio.conversation.common.enums.ConversationsError
import com.twilio.conversation.common.extensions.ConversationsException
import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseTokenManager {

    suspend fun retrieveToken(): String {
        deleteToken().await()

        return suspendCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                try {
                    task.result?.let {
                        continuation.resume(it)
                    }
                        ?: continuation.resumeWithException(ConversationsException(ConversationsError.TOKEN_ERROR))
                } catch (e: Exception) {
                    // TOO_MANY_REGISTRATIONS thrown on devices with too many Firebase instances
                    continuation.resumeWithException(ConversationsException(ConversationsError.TOKEN_ERROR))
                }
            }
        }
    }

    fun deleteToken() = CompletableDeferred<Boolean>().apply {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
            complete(task.isSuccessful)
        }
    }
}