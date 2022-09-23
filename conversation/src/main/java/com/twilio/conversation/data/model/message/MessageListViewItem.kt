package com.twilio.conversation.data.model.message

import android.net.Uri
import com.twilio.conversation.common.enums.*


data class MessageListViewItem(
    val sid: String,
    val uuid: String,
    val index: Long,
    val direction: Direction,
    val author: String,
    val authorChanged: Boolean,
    val body: String,
    val dateCreated: Long,
    val sendStatus: SendStatus,
    val sendStatusIcon: Int,
    val reactions: Reactions,
    val type: MessageType,
    val mediaSid: String?,
    val mediaFileName: String?,
    val mediaType: String?,
    val mediaSize: Long?,
    val mediaUri: Uri?,
    val mediaDownloadId: Long?,
    val mediaDownloadedBytes: Long?,
    val mediaDownloadState: DownloadState,
    val mediaUploading: Boolean,
    val mediaUploadedBytes: Long?,
    val mediaUploadUri: Uri?,
    val errorCode: Int,
    val messageCount: Long = 0,
    val deleteStatus : DeleteStatus,
    val seenStatus: String
) {
    fun compareSender(other: MessageListViewItem): Boolean = when {
        direction == Direction.OUTGOING && other.direction == Direction.OUTGOING -> {
            author == other.author
        }
        direction == Direction.INCOMING && other.direction == Direction.INCOMING -> {
            author == other.author
        }
        else -> false
    }
}
