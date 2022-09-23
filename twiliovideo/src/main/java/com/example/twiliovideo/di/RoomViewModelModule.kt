package com.example.twiliovideo.di

import android.app.Application
import com.example.twiliovideo.participant.ParticipantManager
import com.example.twiliovideo.room.RoomViewState
import com.example.twiliovideo.util.PermissionUtil
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class RoomViewModelModule {

    @Provides
    @ViewModelScoped
    fun providesPermissionUtil(application: Application) = PermissionUtil(application)

    @Provides
    @ViewModelScoped
    fun providesParticipantManager() = ParticipantManager()

    @Provides
    @ViewModelScoped
    fun providesInitialViewState(participantManager: ParticipantManager) = RoomViewState(participantManager.primaryParticipant)

    @Provides
    @ViewModelScoped
    fun providesAudioSwitch(application: Application): AudioSwitch =
        AudioSwitch(application,
            loggingEnabled = true,
            preferredDeviceList = listOf(
                AudioDevice.BluetoothHeadset::class.java,
                AudioDevice.WiredHeadset::class.java,
                AudioDevice.Speakerphone::class.java,
                AudioDevice.Earpiece::class.java))
}
