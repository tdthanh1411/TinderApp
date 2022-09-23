/*
 * Copyright (C) 2020 Twilio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.twiliovideo.room

import com.example.twiliovideo.sdk.VideoTrackViewState
import com.twilio.video.VideoTrack

/**
 * Created by ThanhTran on 6/23/2022.
 */

class PrimaryParticipantController(
    private val primaryView: ParticipantPrimaryView
) {
    private var primaryItem: Item? = null

    fun updatePerson(
        identity: String?,
        imageUrl: String?
    ) {
        primaryView.setIdentityy(identity ?: "")
    }

    fun renderAsPrimary(
        identity: String?,
        screenTrack: VideoTrackViewState?,
        videoTrack: VideoTrackViewState?,
        muted: Boolean,
        mirror: Boolean
    ) {
        val old = primaryItem
        val newItem = Item(
            identity,
            screenTrack?.videoTrack ?: videoTrack?.videoTrack,
            muted,
            mirror
        )
        primaryItem = newItem
        primaryView.setIdentityy(newItem.identity ?: "")
        primaryView.showIdentityBadge(true)
        primaryView.setMuted(newItem.muted)
        primaryView.setMirrored(newItem.mirror)
        primaryView.setMuteMicPrimary(newItem.muted)
        val newVideoTrack = newItem.videoTrack

        // Only update sink for a new video track
        if (newVideoTrack != old?.videoTrack) {
            old?.let { removeSink(it.videoTrack, primaryView) }
            newVideoTrack?.let { if (it.isEnabled) it.addSink(primaryView.getVideoTextureView()) }
        }

        newVideoTrack?.let {
            primaryView.setStated(ParticipantView.State.VIDEO)
        } ?: primaryView.setStated(ParticipantView.State.NO_VIDEO)
    }

    private fun removeSink(videoTrack: VideoTrack?, view: ParticipantView) {
        if (videoTrack == null || !videoTrack.sinks.contains(view.getVideoTextureView())) return
        videoTrack.removeSink(view.getVideoTextureView())
    }

    internal class Item(
        var identity: String?,
        var videoTrack: VideoTrack?,
        var muted: Boolean,
        var mirror: Boolean
    )
}
