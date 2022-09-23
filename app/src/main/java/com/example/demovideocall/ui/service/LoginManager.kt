package com.example.demovideocall.ui.service

import android.util.Log
import com.twilio.conversation.common.enums.ConversationsError
import com.twilio.conversation.common.extensions.registerFCMToken
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.repository.ConversationsRepository
import timber.log.Timber

interface LoginManager {
    suspend fun signIn(identity: String, password: String)

    //        suspend fun signInUsingStoredCredentials()
    suspend fun signOut()
    suspend fun registerForFcm()
    suspend fun unregisterFromFcm()

    fun clearCredentials()
    fun isLoggedIn(): Boolean
}

class LoginManagerImpl(
    private val conversationsClient: ConversationsClient,
    private val conversationsRepository: ConversationsRepository,
    private val firebaseTokenManager: FirebaseTokenManager,
    private val credentialStorage: CredentialStorage
) : LoginManager {

    override suspend fun registerForFcm() {
        try {
            val token = firebaseTokenManager.retrieveToken()
            credentialStorage.fcmToken = token
            conversationsClient.getConversationsClient().registerFCMToken(token)
        } catch (e: Exception) {
            Timber.d("Failed to registerForFcm: ${e.message}")
        }
    }

    override suspend fun unregisterFromFcm() {
        // We don't call `conversationsClient.getConversationsClient().unregisterFCMToken(token)` here
        // because it fails with commandTimeout (60s by default) if device is offline or token is expired.
        // Instead we try to delete token on FCM async. Which leads to the same result if device is online,
        // but we can shutdown `conversationsClient`immediately without waiting a result.
        firebaseTokenManager.deleteToken()
    }

    override suspend fun signIn(identity: String, password: String) {
        conversationsClient.create(identity, password)
        credentialStorage.storeCredentials(identity, password)
        conversationsRepository.subscribeToConversationsClientEvents()
        registerForFcm()
    }

//    override suspend fun signInUsingStoredCredentials() {
//        if (credentialStorage.isEmpty()) throw ConversationsException(NO_STORED_CREDENTIALS)
//
//        val identity = credentialStorage.identity
//        val password = credentialStorage.password
//
//        try {
//            conversationsClient.create(identity, password)
//            conversationsRepository.subscribeToConversationsClientEvents()
//            registerForFcm()
//        } catch (e: ConversationsException) {
//            handleError(e.error)
//            throw e
//        }
//    }

    override suspend fun signOut() {
        unregisterFromFcm()
        clearCredentials()
        conversationsRepository.unsubscribeFromConversationsClientEvents()
        conversationsRepository.clear()
        conversationsClient.shutdown()
    }

    override fun isLoggedIn() = conversationsClient.isClientCreated && !credentialStorage.isEmpty()

    override fun clearCredentials() {
        credentialStorage.clearCredentials()
    }

    private fun handleError(error: ConversationsError) {
        if (error == ConversationsError.TOKEN_ACCESS_DENIED) {
            clearCredentials()
        }
    }
}
