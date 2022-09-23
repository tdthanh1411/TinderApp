package com.example.twiliovideo.data

import android.app.Application
import android.content.SharedPreferences
import com.example.twiliovideo.util.getSharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class DataModule {
    @Provides
    internal fun provideSharedPreferences(app: Application): SharedPreferences {
        return getSharedPreferences(app)
    }
}