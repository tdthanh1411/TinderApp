package com.example.demovideocall.di

import android.app.Application
import android.content.Context
import com.example.demovideocall.ui.service.*
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.repository.ConversationsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by ThanhTran on 7/25/2022.
 */

@Module
@InstallIn(SingletonComponent::class)
class FCMModule {

    @Singleton
    @Provides
    fun providesLoginManager(context: Application): LoginManager = LoginManagerImpl(
        ConversationsClient.INSTANCE,
        ConversationsRepositoryImpl.INSTANCE,
        FirebaseTokenManager(),
        CredentialStorage(context)
    )

    @Singleton
    @Provides
    fun provideFCMManager(context: Application): FCMManager = FCMManagerImpl(
        context,
        ConversationsClient.INSTANCE,
        CredentialStorage(context)
    )
}