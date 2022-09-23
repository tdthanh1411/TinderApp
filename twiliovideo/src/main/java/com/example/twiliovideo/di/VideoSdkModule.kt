package com.example.twiliovideo.di

import android.app.Application
import android.content.SharedPreferences
import com.example.twiliovideo.sdk.ConnectOptionsFactory
import com.example.twiliovideo.sdk.RoomManager
import com.example.twiliovideo.sdk.VideoClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class VideoSdkModule {

    @Provides
    @Singleton
    fun providesRoomManager(
        application: Application,
        sharedPreferences: SharedPreferences
    ): RoomManager {
        val connectOptionsFactory = ConnectOptionsFactory(application, sharedPreferences)
        val videoClient = VideoClient(application, connectOptionsFactory)
        return RoomManager(application, videoClient, sharedPreferences)
    }
}