package com.example.demovideocall.common

import android.app.Application
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import com.twilio.conversation.data.ConversationsClient
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.localCache.LocalCacheProvider
import com.twilio.conversation.repository.ConversationsRepositoryImpl
import dagger.hilt.android.HiltAndroidApp

/**
 * Created by ThanhTran on 6/16/2022.
 */
@HiltAndroidApp
class CallVideoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        ConversationsClient.createInstance(this)
        LocalCacheProvider.createInstance(this)
        ConversationsRepositoryImpl.createInstance(
            ConversationsClient.INSTANCE, LocalCacheProvider.INSTANCE,
            CredentialStorage(this)
        )
        EmojiCompat.init(BundledEmojiCompatConfig(this))

    }
}