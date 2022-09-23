package com.twilio.conversation.di

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.room.Insert
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.localCache.LocalCacheProvider
import com.twilio.conversation.manager.*
import com.twilio.conversation.repository.ConversationsRepositoryImpl
import com.twilio.conversation.ui.message.MessageViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by ThanhTran on 7/7/2022.
 */


@Module
@InstallIn(SingletonComponent::class)
class ConversationModule {

    @Provides
    fun provideContext(app: Application): Context = app.applicationContext


    @Singleton
    @Provides
    fun providesConversationsClientWrapper(): ConversationsClient {
        return ConversationsClient.INSTANCE
    }

    @Singleton
    @Provides
    fun providesCredentialStorage(app: Application) = CredentialStorage(app)

    @Singleton
    @Provides
    fun providesLocalCacheProvider() = LocalCacheProvider.INSTANCE

    @Singleton
    @Provides
    fun providesConversationRepository() = ConversationsRepositoryImpl.INSTANCE


    @Singleton
    @Provides
    fun providesConversationListManager(): ConversationListManager =
        ConversationListManagerImpl(ConversationsClient.INSTANCE)

    @Singleton
    @Provides
    fun providesMessageListManager(): MessageListManager =
        MessageListManagerImpl(ConversationsClient.INSTANCE, ConversationsRepositoryImpl.INSTANCE)


    @Singleton
    @Provides
    fun providesConversationsListManage() =
        ConversationListManagerImpl(ConversationsClient.INSTANCE)

    @Singleton
    @Provides
    fun providesParticipantListManager(): ParticipantListManager =
        ParticipantListManagerImpl(ConversationsClient.INSTANCE)

    @Singleton
    @Provides
    fun provideString() = "Injected String"

    @Singleton
    @Provides
    fun providesLong() = 0L


}