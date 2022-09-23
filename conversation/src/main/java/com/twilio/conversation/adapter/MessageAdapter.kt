package com.twilio.conversation.adapter

import android.app.Activity
import android.graphics.Point
import android.graphics.Typeface
import android.net.Uri
import android.text.format.Formatter
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.giphy.sdk.core.GPHCore
import com.giphy.sdk.ui.utils.px
import com.twilio.conversation.R
import com.twilio.conversation.common.enums.*
import com.twilio.conversation.common.extensions.asLastMessageDateString
import com.twilio.conversation.common.extensions.extractUrl
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.message.MessageListViewItem
import com.twilio.conversation.data.model.previewurl.OpenGraphResult
import com.twilio.conversation.databinding.ItemMessageIncomingBinding
import com.twilio.conversation.databinding.ItemMessageOutgoingBinding
import com.twilio.conversation.databinding.ItemReactionBinding
import com.twilio.conversation.manager.MessageListManager
import com.twilio.conversation.ui.message.BubbleUtils
import com.twilio.conversation.ui.message.BubbleUtils.canGroup
import com.twilio.conversation.ui.message.BubbleUtils.getBubble
import com.twilio.conversation.ui.message.BubbleUtils.getVisibleAvatar
import com.twilio.conversation.ui.message.BubbleUtils.getVisibleDateTime
import com.twilio.conversation.ui.message.MessageDiffUtil
import com.twilio.conversation.ui.message.previewurl.OpenGraphCallback
import com.twilio.conversation.ui.message.previewurl.OpenGraphParser
import kotlinx.android.synthetic.main.item_message_incoming.view.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


/**
 * Created by ThanhTran on 7/8/2022.
 */

@OptIn(DelicateCoroutinesApi::class)
class MessageAdapter(
    private val context: Activity,
    private val conversationSid: String,
    private val messageListManager: MessageListManager,
    private val onDisplaySendError: (message: MessageListViewItem) -> Unit,
    private val onDownloadMedia: (message: MessageListViewItem) -> Unit,
    private val onOpenMedia: (location: Uri, mimeType: String) -> Unit,
    private val onItemLongClick: (messageIndex: Long, p: Point, reactions: Reactions, messageType: Int, seenStatus: String) -> Unit,
    private val onReactionClicked: (messageIndex: Long) -> Unit,
    private val onPreviewUrlClicked: (url: String) -> Unit,
    private val onPreviewPhotoClicked: (url: String) -> Unit,
    private val credentialStorage: CredentialStorage,
    private val isGroup: Boolean
) : PagedListAdapter<MessageListViewItem, RecyclerView.ViewHolder>(MessageDiffUtil()) {

    private lateinit var openGraphParser: OpenGraphParser

    private var seenIndex = 0L

    fun getMessage(position: Int): MessageListViewItem? {
        return getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == Direction.INCOMING.value) {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = ItemMessageIncomingBinding.inflate(layoutInflater, parent, false)
            IncomingItemViewHolder(view)
        } else {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = ItemMessageOutgoingBinding.inflate(layoutInflater, parent, false)
            OutgoingItemViewHolder(view)
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position) ?: return
        val previous = if (position == 0) null else getItem(position - 1)
        val next = if (position == itemCount - 1) null else getItem(position + 1)

        val mediaSize = Formatter.formatShortFileSize(context, message.mediaSize ?: 0)
        val mediaUploadedBytes =
            Formatter.formatShortFileSize(context, message.mediaUploadedBytes ?: 0)
        val mediaDownloadedBytes =
            Formatter.formatShortFileSize(context, message.mediaDownloadedBytes ?: 0)

        if (message.seenStatus == SeenStatusType.SEEN.type) {
            if (seenIndex < message.index) {
                seenIndex = message.index
            }
        }

        val attachmentInfoText = when {
            message.sendStatus == SendStatus.ERROR -> context.getString(R.string.err_failed_to_upload_media)

            message.mediaUploading -> context.getString(
                R.string.attachment_uploading,
                mediaUploadedBytes
            )

            message.mediaUploadUri != null ||
                    message.mediaDownloadState == DownloadState.COMPLETED -> context.getString(R.string.attachment_tap_to_open)

            message.mediaDownloadState == DownloadState.NOT_STARTED -> mediaSize

            message.mediaDownloadState == DownloadState.DOWNLOADING -> context.getString(
                R.string.attachment_downloading,
                mediaDownloadedBytes
            )

            message.mediaDownloadState == DownloadState.ERROR -> context.getString(R.string.err_failed_to_download_media)

            else -> error("Never happens")
        }

        val attachmentInfoColor = when {
            message.sendStatus == SendStatus.ERROR ||
                    message.mediaDownloadState == DownloadState.ERROR -> ContextCompat.getColor(
                context,
                R.color.red
            )

            message.mediaUploading -> ContextCompat.getColor(context, R.color.color_text_subtitle)

            message.mediaUploadUri != null ||
                    message.mediaDownloadState == DownloadState.COMPLETED -> ContextCompat.getColor(
                context,
                R.color.blue
            )

            else -> ContextCompat.getColor(context, R.color.color_text_subtitle)
        }

        val attachmentOnClickListener = View.OnClickListener {
            if (message.mediaDownloadState == DownloadState.COMPLETED && message.mediaUri != null && message.mediaType != null) {
                onOpenMedia(message.mediaUri, message.mediaType)
            } else if (message.mediaUploadUri != null && message.mediaType != null) {
                onOpenMedia(message.mediaUploadUri, message.mediaType)
            } else if (message.mediaDownloadState != DownloadState.DOWNLOADING) {
                onDownloadMedia(message)
            }
        }

        if (message.sendStatus == SendStatus.ERROR) {
            holder.itemView.setOnClickListener {
                onDisplaySendError(message)
            }
        }

        with(holder.itemView) {
            val timeSincePrevious =
                TimeUnit.MILLISECONDS.toMinutes(message.dateCreated - (previous?.dateCreated ?: 0))

            tvMessageDateTime.isVisible = timeSincePrevious >= BubbleUtils.TIMESTAMP_THRESHOLD

            tvMessageBody.setBackgroundResource(
                getBubble(
                    canGroupWithPrevious = canGroup(message, previous),
                    canGroupWithNext = canGroup(message, next),
                    isMe = message.direction == Direction.OUTGOING
                )
            )

            groupSpace.isVisible = getVisibleDateTime(
                canGroupWithPrevious = canGroup(message, previous),
                canGroupWithNext = canGroup(message, next)
            )



            tvMessageBody.text = message.body
            tvMessageBody.setTypeface(null, Typeface.NORMAL)
            tvMessageBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)

            val listUrl = message.body.extractUrl()
            if (listUrl.isNotEmpty()) {
                var url = listUrl.first()
                openGraphParser = OpenGraphParser(object : OpenGraphCallback {
                    override fun onPostResponse(openGraphResult: OpenGraphResult) {
                        if (openGraphResult.image.isNullOrEmpty()) {
                            imgPreviewUrl.isVisible = false
                            imgPreview.isVisible = false
                        } else {
                            imgPreviewUrl.isVisible = true
                            imgPreview.isVisible = true
                            Glide.with(context).load(openGraphResult.image).into(imgPreviewUrl)
                            Glide.with(context).load(openGraphResult.image).into(imgPreview)
                        }

                        tvTitlePreviewUrl.text = openGraphResult.title
                        tvDescriptionPreviewUrl.text = openGraphResult.description
                        tvUrl.text = openGraphResult.url

                    }

                    override fun onError(error: String) {
                        cardViewPreviewUrl.isVisible = false
                        tvMessageBody.isVisible = true

                    }

                }, showNullOnEmpty = true)
                openGraphParser.parse(url)
                if (!url.contains("http")) {
                    url = "http://$url"
                }
                cardViewPreviewUrl.setOnClickListener {
                    onPreviewUrlClicked(url)
                }
            }

            tvAttachmentFileName.text = message.mediaFileName
                ?: context.resources.getString(R.string.attachment_file_name_unknown)

            progressAttachment.isVisible =
                message.mediaDownloadState == DownloadState.DOWNLOADING


            imgAttachmentFailed.isVisible =
                message.mediaDownloadState == DownloadState.ERROR

            tvMessageDateTime.text = message.dateCreated.asLastMessageDateString(context)

            tvAttachmentInfo.text = attachmentInfoText
            tvAttachmentInfo.setTextColor(attachmentInfoColor)


            clAttachment.setOnClickListener(attachmentOnClickListener)
            addReactions(llMessageReactionBody, message)
            addReactions(llMessageReactionGif, message)
            addReactions(llMessageReactionAttachment, message)
            addReactions(llMessageReactionPhoto, message)
            addReactions(llMessageReactionPreviewUrl, message)

            if (message.body.startsWith(conversationSid)) {
                val idMedia = message.body.replace(conversationSid, "")
                GPHCore.gifById(idMedia) { result, e ->
                    result?.data?.let { media ->
                        gphVideoMediaView.setMedia(media)
                        gphVideoMediaView.cornerRadius = 4.px.toFloat()
                        gphVideoMediaView.isBackgroundVisible = false
                    }

                }

            }

        }

        when (holder) {
            is IncomingItemViewHolder -> {
                with(holder.binding) {
                    imgMessageAvatar.visibility = getVisibleAvatar(
                        canGroupWithPrevious = canGroup(message, previous),
                        canGroupWithNext = canGroup(message, next)
                    )

                    if (message.mediaDownloadState == DownloadState.COMPLETED) {
                        imgAttachmentIcon.setImageResource(R.drawable.ic_attachment_downloaded)
                    } else {
                        imgAttachmentIcon.setImageResource(R.drawable.ic_attachment_to_download)
                    }

                    if (message.type == MessageType.MEDIA && message.mediaType != null) {
                        if (message.mediaType.startsWith("image/") || message.mediaType.startsWith("jpeg")) {
                            GlobalScope.launch(Dispatchers.Main) {
                                val path = runCatching {
                                    messageListManager.getMediaContentTemporaryUrl(
                                        conversationSid,
                                        message.index
                                    )
                                }
                                path.onSuccess { value ->
                                    if (!context.isFinishing) {
                                        Glide.with(context)
                                            .asBitmap()
                                            .placeholder(R.drawable.placeholder)
                                            .load(value)
                                            .into(object :
                                                BitmapImageViewTarget(imgAttachmentThumbnail) {
                                            })
                                    }

                                    imgAttachmentThumbnail.setOnClickListener {
                                        onPreviewPhotoClicked(value)
                                    }
                                    imgAttachmentThumbnail.setOnLongClickListener {
                                        if (message.deleteStatus.type != TypeDeleteMessage.TYPE_DELETE_ALL.type) {
                                            onItemLongClick(
                                                message.index,
                                                Point(
                                                    holder.itemView.x.toInt(),
                                                    holder.itemView.y.toInt()
                                                ), message.reactions,
                                                message.direction.value,
                                                message.seenStatus
                                            )
                                        }

                                        return@setOnLongClickListener true
                                    }
                                }
                            }
                            imgAttachmentThumbnail.isVisible = true
                            clAttachment.isVisible = false
                            tvMessageBody.isVisible = false
                            cardViewPreviewUrl.isVisible = false
                            llGifPhy.isVisible = false

                            llMessageReactionBody.isVisible = false
                            llMessageReactionAttachment.isVisible = false
                            llMessageReactionPhoto.isVisible = true
                            llMessageReactionGif.isVisible = false
                            llMessageReactionPreviewUrl.isVisible = false

                        } else {
                            imgAttachmentThumbnail.isVisible = false
                            clAttachment.isVisible = true
                            tvMessageBody.isVisible = false
                            cardViewPreviewUrl.isVisible = false
                            llGifPhy.isVisible = false

                            llMessageReactionBody.isVisible = false
                            llMessageReactionAttachment.isVisible = true
                            llMessageReactionPhoto.isVisible = false
                            llMessageReactionGif.isVisible = false
                            llMessageReactionPreviewUrl.isVisible = false
                        }
                    } else {
                        imgAttachmentThumbnail.isVisible = false
                        clAttachment.isVisible = false


                        llMessageReactionAttachment.isVisible = false
                        llMessageReactionPhoto.isVisible = false


                        val listUrl = message.body.extractUrl()
                        if (listUrl.isNotEmpty()) {
                            tvMessageBody.isVisible = false
                            llGifPhy.isVisible = false
                            cardViewPreviewUrl.isVisible = true

                            llMessageReactionBody.isVisible = false
                            llMessageReactionGif.isVisible = false
                            llMessageReactionPreviewUrl.isVisible = true
                        } else {
                            if (message.body.startsWith(conversationSid)) {
                                llGifPhy.isVisible = true
                                tvMessageBody.isVisible = false

                                llMessageReactionBody.isVisible = false
                                llMessageReactionGif.isVisible = true
                            } else {
                                tvMessageBody.isVisible = true
                                llGifPhy.isVisible = false

                                llMessageReactionBody.isVisible = true
                                llMessageReactionGif.isVisible = false
                            }
                            cardViewPreviewUrl.isVisible = false
                            llMessageReactionPreviewUrl.isVisible = false

                        }

                    }

                    when (message.deleteStatus.type) {
                        TypeDeleteMessage.TYPE_DELETE_FOR_YOU.type -> {
                            if (message.deleteStatus.identity == credentialStorage.identity) {
                                tvMessageBody.text =
                                    context.resources.getString(R.string.message_has_been_deleted)
                                tvMessageBody.setBackgroundResource(R.drawable.bg_delete_message)


                                tvMessageBody.setTypeface(
                                    tvMessageBody.typeface,
                                    Typeface.ITALIC
                                )
                                tvMessageBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                tvMessageBody.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.color_gray
                                    )
                                )

                                tvMessageBody.isVisible = true
                                imgAttachmentThumbnail.isVisible = false
                                clAttachment.isVisible = false
                                cardViewPreviewUrl.isVisible = false
                                llGifPhy.isVisible = false
                            }
                        }
                        TypeDeleteMessage.TYPE_DELETE_ALL.type -> {
                            tvMessageBody.text =
                                context.resources.getString(R.string.message_has_been_deleted)
                            tvMessageBody.setBackgroundResource(R.drawable.bg_delete_message)


                            tvMessageBody.setTypeface(tvMessageBody.typeface, Typeface.ITALIC)
                            tvMessageBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)

                            tvMessageBody.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.color_gray
                                )
                            )

                            tvMessageBody.isVisible = true
                            imgAttachmentThumbnail.isVisible = false
                            clAttachment.isVisible = false
                            cardViewPreviewUrl.isVisible = false
                            llGifPhy.isVisible = false
                        }
                        else -> {
                            tvMessageBody.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.black
                                )
                            )
                        }
                    }

                }
            }
            is OutgoingItemViewHolder -> {
                with(holder.binding) {


                    if (message.mediaDownloadState == DownloadState.COMPLETED || message.mediaUploadUri != null) {
                        imgAttachmentIcon.setImageResource(R.drawable.ic_attachment_downloaded)
                    } else {
                        imgAttachmentIcon.setImageResource(R.drawable.ic_attachment_to_download)

                    }

                    if (message.index > seenIndex) {
                        imgMessageSendStatus.setImageResource(message.sendStatusIcon)
                        imgMessageSendStatus.visibility = View.VISIBLE
                    } else if (message.index == seenIndex) {
                        imgMessageSendStatus.setImageResource(R.drawable.ic_avatar)
                        imgMessageSendStatus.visibility = View.VISIBLE
                    } else {
                        imgMessageSendStatus.visibility = View.INVISIBLE
                    }


                    tvMessageSendError.isVisible = message.sendStatus == SendStatus.ERROR


                    if (message.type == MessageType.MEDIA && message.mediaType != null) {
                        if (message.mediaType.startsWith("image/") || message.mediaType.startsWith("jpeg")) {
                            GlobalScope.launch(Dispatchers.Main) {
                                val path = runCatching {
                                    messageListManager.getMediaContentTemporaryUrl(
                                        conversationSid,
                                        message.index
                                    )
                                }
                                path.onSuccess { value ->
                                    if (!context.isFinishing) {
                                        Glide.with(context)
                                            .asBitmap()
                                            .placeholder(R.drawable.placeholder)
                                            .load(value)
                                            .into(object :
                                                BitmapImageViewTarget(imgAttachmentThumbnail) {
                                            })
                                    }

                                    imgAttachmentThumbnail.setOnClickListener {
                                        onPreviewPhotoClicked(value)
                                    }
                                    imgAttachmentThumbnail.setOnLongClickListener {
                                        if (message.deleteStatus.type != TypeDeleteMessage.TYPE_DELETE_ALL.type) {
                                            onItemLongClick(
                                                message.index,
                                                Point(
                                                    holder.itemView.x.toInt(),
                                                    holder.itemView.y.toInt()
                                                ), message.reactions,
                                                message.direction.value,
                                                message.seenStatus
                                            )
                                        }
                                        return@setOnLongClickListener true
                                    }

                                }
                            }
                            tvMessageBody.isVisible = message.type != MessageType.MEDIA
                            imgAttachmentThumbnail.isVisible = true
                            clAttachment.isVisible = false
                            llGifPhy.isVisible = false

                            llMessageReactionBody.isVisible = false
                            llMessageReactionAttachment.isVisible = false
                            llMessageReactionPhoto.isVisible = true
                            llMessageReactionGif.isVisible = false
                            llMessageReactionPreviewUrl.isVisible = false

                        } else {
                            tvMessageBody.isVisible = message.type != MessageType.MEDIA
                            imgAttachmentThumbnail.isVisible = false
                            clAttachment.isVisible = true
                            llGifPhy.isVisible = false

                            llMessageReactionBody.isVisible = false
                            llMessageReactionAttachment.isVisible = true
                            llMessageReactionPhoto.isVisible = false
                            llMessageReactionGif.isVisible = false
                            llMessageReactionPreviewUrl.isVisible = false

                        }
                    } else {
                        val listUrl = message.body.extractUrl()
                        if (listUrl.isNotEmpty()) {
                            tvMessageBody.isVisible = false
                            cardViewPreviewUrl.isVisible = true

                            llMessageReactionBody.isVisible = false
                            llMessageReactionGif.isVisible = false
                            llMessageReactionPreviewUrl.isVisible = true
                        } else {
                            if (message.body.startsWith(conversationSid)) {
                                llGifPhy.isVisible = true
                                tvMessageBody.isVisible = false

                                llMessageReactionBody.isVisible = false
                                llMessageReactionGif.isVisible = true
                            } else {
                                tvMessageBody.isVisible = true
                                llGifPhy.isVisible = false

                                llMessageReactionBody.isVisible = true
                                llMessageReactionGif.isVisible = false
                            }
                            llMessageReactionPreviewUrl.isVisible = false
                            cardViewPreviewUrl.isVisible = false
                        }
                        imgAttachmentThumbnail.isVisible = false
                        clAttachment.isVisible = false

                        llMessageReactionAttachment.isVisible = false
                        llMessageReactionPhoto.isVisible = false


                    }


                    when (message.deleteStatus.type) {
                        TypeDeleteMessage.TYPE_DELETE_FOR_YOU.type -> {
                            if (message.deleteStatus.identity == credentialStorage.identity) {
                                tvMessageBody.text =
                                    context.resources.getString(R.string.message_has_been_deleted)
                                tvMessageBody.setBackgroundResource(R.drawable.bg_delete_message)


                                tvMessageBody.setTypeface(
                                    tvMessageBody.typeface,
                                    Typeface.ITALIC
                                )
                                tvMessageBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                tvMessageBody.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.color_gray
                                    )
                                )

                                tvMessageBody.isVisible = true
                                imgAttachmentThumbnail.isVisible = false
                                clAttachment.isVisible = false
                                cardViewPreviewUrl.isVisible = false
                                llGifPhy.isVisible = false
                            }
                        }
                        TypeDeleteMessage.TYPE_DELETE_ALL.type -> {
                            tvMessageBody.text =
                                context.resources.getString(R.string.message_has_been_deleted)
                            tvMessageBody.setBackgroundResource(R.drawable.bg_delete_message)


                            tvMessageBody.setTypeface(tvMessageBody.typeface, Typeface.ITALIC)
                            tvMessageBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)

                            tvMessageBody.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.color_gray
                                )
                            )

                            tvMessageBody.isVisible = true
                            imgAttachmentThumbnail.isVisible = false
                            clAttachment.isVisible = false
                            cardViewPreviewUrl.isVisible = false
                            llGifPhy.isVisible = false
                        }
                        else -> {
                            tvMessageBody.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.white
                                )
                            )
                        }
                    }

                }

            }
        }


        val longClickListener = View.OnLongClickListener {
            if (message.deleteStatus.type != TypeDeleteMessage.TYPE_DELETE_ALL.type) {
                onItemLongClick(
                    message.index,
                    Point(holder.itemView.x.toInt(), holder.itemView.y.toInt()), message.reactions,
                    message.direction.value,
                    message.seenStatus
                )
            }
            return@OnLongClickListener true
        }


        holder.itemView.setOnLongClickListener(longClickListener)
        holder.itemView.llGifPhy.setOnLongClickListener(longClickListener)


    }


    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.direction?.value ?: Direction.OUTGOING.value
    }

    class IncomingItemViewHolder(val binding: ItemMessageIncomingBinding) :
        RecyclerView.ViewHolder(binding.root)


    class OutgoingItemViewHolder(val binding: ItemMessageOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root)


    private fun addReactions(rootView: LinearLayout, message: MessageListViewItem) {
        rootView.setOnClickListener { onReactionClicked(message.index) }
        rootView.removeAllViews()
        message.reactions.forEach { reaction ->
            if (reaction.value.isNotEmpty()) {
                val emoji = ItemReactionBinding.inflate(LayoutInflater.from(rootView.context))
                emoji.emojiIcon.setText(reaction.key.emoji)
                emoji.emojiCounter.text = reaction.value.size.toString()

                emoji.emojiCounter.isVisible = isGroup

                val color =
                    if (message.direction == Direction.OUTGOING) R.color.black else R.color.blue
                emoji.emojiCounter.setTextColor(ContextCompat.getColor(rootView.context, color))

                rootView.addView(emoji.root)
            }
        }
    }

}