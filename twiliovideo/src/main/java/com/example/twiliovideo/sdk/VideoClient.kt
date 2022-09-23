package com.example.twiliovideo.sdk

import android.content.Context
import com.example.twiliovideo.sdk.ConnectOptionsFactory
import com.twilio.video.Room
import com.twilio.video.Video

class VideoClient(
    private val context: Context,
    private val connectOptionsFactory: ConnectOptionsFactory
) {
    suspend fun connect(
        token: String,
        roomName: String,
        roomListener: Room.Listener
    ): Room {

            return Video.connect(
                    context,
                    connectOptionsFactory.newInstance(token, roomName),
                    roomListener)
    }
}