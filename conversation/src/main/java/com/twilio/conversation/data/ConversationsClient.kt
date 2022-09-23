package com.twilio.conversation.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.twilio.conversations.ConversationsClient
import kotlinx.coroutines.*
import java.io.FileNotFoundException
import java.net.URL
import com.twilio.conversation.common.enums.ConversationsError.TOKEN_ERROR
import com.twilio.conversation.common.enums.ConversationsError.TOKEN_ACCESS_DENIED
import com.twilio.conversation.common.extensions.ConversationsException
import com.twilio.conversation.common.extensions.addListener
import com.twilio.conversation.common.extensions.createAndSyncClient
import com.twilio.conversation.common.extensions.updateToken


class ConversationsClient(private val applicationContext: Context) {

    private var deferredClient = CompletableDeferred<ConversationsClient>()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val isClientCreated get() = deferredClient.isCompleted && !deferredClient.isCancelled

    val onUpdateTokenFailure = mutableListOf<() -> Unit>()

    private fun notifyUpdateTokenFailure() = onUpdateTokenFailure.forEach { it() }

    suspend fun getConversationsClient() =
        deferredClient.await() // Business logic will wait until conversationsClient created

    /**
     * Get token and call createClient if token is not null
     */
    suspend fun create(identity: String, password: String) {

        val token = getToken(identity, password)

        val client = createAndSyncClient(applicationContext, token)
        this.deferredClient.complete(client)


        client.addListener(
            onTokenAboutToExpire = { updateToken(identity, password, notifyOnFailure = false) },
            onTokenExpired = { updateToken(identity, password, notifyOnFailure = true) },
        )
    }

    suspend fun shutdown() {
        getConversationsClient().shutdown()
        deferredClient = CompletableDeferred()
    }

    /**
     * Fetch Twilio access token and return it, if token is non-null, otherwise return error
     */
    private suspend fun getToken(username: String, password: String) = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(TOKEN_URL)
                .buildUpon()
                .appendQueryParameter(QUERY_IDENTITY, username)
                .appendQueryParameter(QUERY_PASSWORD, password)
                .appendQueryParameter(QUERY_DEVICE_TYPE, "ANDROID")
                .build()
                .toString()

            return@withContext URL(uri).readText()
        } catch (e: FileNotFoundException) {
            throw ConversationsException(TOKEN_ACCESS_DENIED)
        } catch (e: Exception) {
            throw ConversationsException(TOKEN_ERROR)
        }
    }

    private fun updateToken(identity: String, password: String, notifyOnFailure: Boolean) =
        coroutineScope.launch {
            val result = runCatching {
                val twilioToken = getToken(identity, password)
                getConversationsClient().updateToken(twilioToken)
            }

            if (result.isFailure && notifyOnFailure) {
                notifyUpdateTokenFailure()
            }
        }

    companion object {
        private const val TOKEN_URL = "https://demo-tinder-4342.twil.io/path_1"
        private const val QUERY_IDENTITY = "identity"
        private const val QUERY_PASSWORD = "password"
        private const val QUERY_DEVICE_TYPE = "deviceType"


        val INSTANCE
            get() = _instance ?: error("call ConversationsClientWrapper.createInstance() first")

        private var _instance: com.twilio.conversation.data.ConversationsClient? = null

        fun createInstance(applicationContext: Context) {
            check(_instance == null) { "ConversationsClientWrapper singleton instance has been already created" }
            _instance = ConversationsClient(applicationContext)
        }

        @DelicateCoroutinesApi
        @RestrictTo(Scope.TESTS)
        fun recreateInstance(applicationContext: Context) {
            _instance?.let { instance ->
                // Shutdown old client if it will ever be created
                GlobalScope.launch { instance.getConversationsClient().shutdown() }
            }

            _instance = null
            createInstance(applicationContext)
        }
    }
}