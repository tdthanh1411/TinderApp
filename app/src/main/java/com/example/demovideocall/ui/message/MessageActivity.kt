package com.example.demovideocall.ui.message

import android.Manifest
import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.demovideocall.common.Constants.TOKEN_GIPHY
import com.example.demovideocall.ui.message.previewphoto.PreviewPhotoActivity
import com.example.demovideocall.ui.room.RoomActivity
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.GPHContentType
import com.giphy.sdk.ui.GPHSettings
import com.giphy.sdk.ui.Giphy
import com.giphy.sdk.ui.themes.GPHTheme
import com.giphy.sdk.ui.themes.GridType
import com.giphy.sdk.ui.utils.VideoCache
import com.giphy.sdk.ui.views.GiphyDialogFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.twilio.conversation.R
import com.twilio.conversation.adapter.MessageAdapter
import com.twilio.conversation.avatarview.load.coil.loadImage
import com.twilio.conversation.common.enums.ConversationsError
import com.twilio.conversation.common.enums.MessageType
import com.twilio.conversation.common.enums.Reactions
import com.twilio.conversation.common.enums.SeenStatusType
import com.twilio.conversation.common.extensions.getString
import com.twilio.conversation.common.extensions.onSubmit
import com.twilio.conversation.common.extensions.showToast
import com.twilio.conversation.data.CredentialStorage
import com.twilio.conversation.data.model.conversation.Deleted
import com.twilio.conversation.data.model.message.MessageListViewItem
import com.twilio.conversation.databinding.ActivityMesageBinding
import com.twilio.conversation.databinding.ViewActionBinding
import com.twilio.conversation.manager.ConversationListManager
import com.twilio.conversation.manager.MessageListManager
import com.twilio.conversation.repository.ConversationsRepository
import com.twilio.conversation.ui.message.MessageViewModel
import com.twilio.conversation.ui.message.crop.CropImageActivity
import com.twilio.conversation.ui.message.reaction.MessageDeleteDialog
import com.twilio.conversation.utils.ExtraUtils.IMAGE_EXTRA_URI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class MessageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONVERSATION_SID = "EXTRA_CONVERSATION_SID"
        const val EXTRA_CONVERSATION_NAME = "EXTRA_CONVERSATION_NAME"
        const val EXTRA_CONVERSATION_AVATAR = "EXTRA_CONVERSATION_AVATAR"
        const val EXTRA_PARTICIPANT_COUNT = "EXTRA_PARTICIPANT_COUNT"
        const val EXTRA_CONVERSATION_MESSAGE_INDEX = "EXTRA_CONVERSATION_MESSAGE_INDEX"
        const val CAMERA_PERMISSION = 100

        fun start(
            context: Context,
            conversationSid: String,
            conversationTitle: String,
            messageIndex: Long,
            participantCount: Int,
            avatarList: List<String>
        ) =
            context.startActivity(
                getStartIntent(
                    context,
                    conversationSid,
                    conversationTitle,
                    messageIndex,
                    participantCount,
                    avatarList
                )
            )

        fun getStartIntent(
            context: Context,
            conversationSid: String,
            conversationTitle: String,
            messageIndex: Long,
            participantCount: Int,
            avatarList: List<String>
        ) = Intent(context, MessageActivity::class.java).apply {
            putExtra(
                EXTRA_CONVERSATION_SID,
                conversationSid
            )
            putExtra(
                EXTRA_CONVERSATION_NAME,
                conversationTitle
            )
            putExtra(
                EXTRA_CONVERSATION_MESSAGE_INDEX,
                messageIndex
            )
            putExtra(
                EXTRA_PARTICIPANT_COUNT,
                participantCount
            )
            putExtra(EXTRA_CONVERSATION_AVATAR, avatarList as Serializable)
        }
    }

    @Inject
    lateinit var conversationsRepository: ConversationsRepository

    @Inject
    lateinit var messageListManager: MessageListManager

    @Inject
    lateinit var credentialStorage: CredentialStorage

    @Inject
    lateinit var conversationsListManager: ConversationListManager

    private lateinit var binding: ActivityMesageBinding

    private val conversationSid by lazy { intent.getStringExtra(EXTRA_CONVERSATION_SID) }
    private val conversationName by lazy { intent.getStringExtra(EXTRA_CONVERSATION_NAME)}
    private val participantCount by lazy { intent.getIntExtra(EXTRA_PARTICIPANT_COUNT, 1) }
    private val messageIndex by lazy { intent.getLongExtra(EXTRA_CONVERSATION_MESSAGE_INDEX, 0L) }
    private val avatar by lazy { intent.getSerializableExtra(EXTRA_CONVERSATION_AVATAR) }


    private var settings = GPHSettings(
        gridType = GridType.waterfall,
        theme = GPHTheme.Light,
        stickerColumnCount = 3
    )
    var contentType = GPHContentType.gif

    private val changeSortPopUp by lazy { PopupWindow(this) }
    private lateinit var reactions: Reactions
    private var messageType = 0
    private var seenStatus = SeenStatusType.UNSEEN.type


    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var storageReference: StorageReference

    private val messageViewModel: MessageViewModel by viewModels<MessageViewModel> {
        MessageViewModel.MessageViewModelFactory(
            this,
            conversationSid ?: "",
            messageIndex,
            conversationsRepository,
            messageListManager,
            credentialStorage,
            conversationsListManager
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMesageBinding.inflate(layoutInflater)

        Giphy.configure(this, TOKEN_GIPHY, true)
        VideoCache.initialize(this, 100 * 1024 * 1024)

        setContentView(binding.root)

        initFirebase()
        initViews()
        initListener()

    }

    private fun initFirebase() {
        firebaseStorage = Firebase.storage
        storageReference = firebaseStorage.reference
    }

    private fun initListener() {
        with(binding) {
            viewHideAction.setOnClickListener {
                changeSortPopUp.dismiss()
                hideDialogAction()
            }
            imgBack.setOnClickListener {
                onBackPressed()
            }

            imgPhoto.setOnClickListener {

                if (checkPermissionCamera()) {
                    startImageCapture()
                } else {
                    requestPermissionCamera()
                }
            }

            imgGallery.setOnClickListener {
                pickImageGallery.launch("image/*")
            }

            edtMessageInput.onSubmit {
                sendMessage()
            }
            imgSend.setOnClickListener {
                sendMessage()
            }

            edtMessageInput.doAfterTextChanged {
                messageViewModel.typing()
            }
            imgVideoCall.setOnClickListener {
                startVideoCall()
            }
            imgCallAudio.setOnClickListener {
                startVideoCall()
            }

            imgGifPhy.setOnClickListener {
                val dialog =
                    GiphyDialogFragment.newInstance(settings.copy(selectedContentType = contentType))
                dialog.gifSelectionListener = getGifSelectionListener()
                dialog.show(supportFragmentManager, "gifs_dialog")
            }


            layoutActionMessage.llRemove.setOnClickListener {
                showMessageActionsDialog(reactions, messageType)
                hideDialogAction()
                changeSortPopUp.dismiss()
            }
            layoutActionMessage.llCopy.setOnClickListener {
                messageViewModel.copyMessageToClipboard()
                hideDialogAction()
                changeSortPopUp.dismiss()

            }

            imgAvatar.setOnClickListener {
                uploadImageToFirebase.launch("image/*")
                progressBarUploadAvatar.isVisible = true
            }

        }
    }


    private fun getGifSelectionListener() = object : GiphyDialogFragment.GifSelectionListener {
        override fun onGifSelected(
            media: Media,
            searchTerm: String?,
            selectedContentType: GPHContentType
        ) {
            messageViewModel.sendTextMessage(conversationSid.plus(media.id))
            val deleteAttribute = messageViewModel.deletedAttribute.value
            val avatarUrl = messageViewModel.imageAvatarUrl.value
            val list: MutableList<Deleted> = arrayListOf()
            list.clear()
            if (!deleteAttribute.isNullOrEmpty()) {
                deleteAttribute.forEach { deleted ->
                    list.add(Deleted(deleted.messageIndex, false, deleted.identity))
                }
            }
            if (!list.isNullOrEmpty()) {
                conversationSid?.let {
                    messageViewModel.setAttributeConversations(it, list, 0, avatarUrl ?: "")
                }
            }

            contentType = selectedContentType
        }

        override fun onDismissed(selectedContentType: GPHContentType) {
            contentType = selectedContentType
        }

        override fun didSearchTerm(term: String) {
        }
    }


    private fun startVideoCall() {
        val intent = Intent(this, RoomActivity::class.java)
        startActivity(intent)
    }

    private fun initViews() {
        with(binding) {
            imgAvatar.loadImage(avatar as List<String>) {
                crossfade(true)
                crossfade(300)
                lifecycle(this@MessageActivity)
            }

            imgAvatar.isClickable = participantCount > 2

            tvNameIdentity.text = "ROOM_NAME"
        }

        val adapter = MessageAdapter(
            context = this,
            conversationSid = conversationSid ?: "",
            messageListManager,
            onDisplaySendError = { message ->
                showSendErrorDialog(message)
            },
            onDownloadMedia = { message ->
                messageViewModel.startMessageMediaDownload(message.index, message.mediaFileName)
            },
            onOpenMedia = { uri, mimeType ->
                if (mimeType.startsWith("image/") || mimeType.startsWith("jpeg")) {
                    viewUri(uri)
                } else {
                    shareUri(uri, mimeType)
                }
            },
            onItemLongClick = { messageIndex, point, reactions, messageType, seenStatus ->
                messageViewModel.selectedMessageIndex = messageIndex
                this.reactions = reactions
                this.messageType = messageType
                this.seenStatus = seenStatus

                showSortPopup(point)
                showAction()
            },
            onReactionClicked = { messageIndex ->
                messageViewModel.selectedMessageIndex = messageIndex
            },
            onPreviewUrlClicked = {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                startActivity(browserIntent)
            },
            onPreviewPhotoClicked = {
                PreviewPhotoActivity.start(this, it)
            },
            credentialStorage,
            participantCount > 2
        )
        binding.messageList.adapter = adapter
        binding.messageList.setHasFixedSize(true)
        binding.messageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val index =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                if (index == RecyclerView.NO_POSITION) return
                val message = adapter.getMessage(index)
                message?.let {
                    messageViewModel.handleMessageDisplayed(it.index)
                    messageViewModel.selectedMessageIndex = it.index
                    messageViewModel.setAttributes(
                        it.reactions,
                        it.deleteStatus.type,
                        SeenStatusType.SEEN.type
                    )
                }
            }
        })



        messageViewModel.messageItems.observe(this) { messages ->
            val lastVisibleMessageIndex =
                (binding.messageList.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
            // Scroll list to bottom when it was at the bottom before submitting new messages
            val commitCallback = if (lastVisibleMessageIndex == adapter.itemCount - 1) {
                Runnable { binding.messageList.scrollToPosition(adapter.itemCount - 1) }
            } else {
                null
            }
            adapter.submitList(messages, commitCallback)
            adapter.notifyDataSetChanged()

        }

        messageViewModel.onShowRemoveMessageDialog.observe(this) {
            showRemoveMessageDialog()
        }
        messageViewModel.onMessageError.observe(this) { error ->
            if (error == ConversationsError.CONVERSATION_GET_FAILED) {
                finish()
            }
            if (error == ConversationsError.MESSAGE_SEND_FAILED) { // shown in message list inline
                return@observe
            }
        }
        messageViewModel.typingParticipantsList.observe(this) { participants ->
            binding.tvTyping.isVisible = participants.isNotEmpty()
            val text = if (participants.size == 1) participants[0] else participants.size.toString()
            binding.tvTyping.text =
                resources.getQuantityString(R.plurals.typing_indicator, participants.size, text)
        }

    }

    private val pickImageGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                CropImageActivity.start(this, it)
            }
        }

    private val uploadImageToFirebase =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { uri ->
                val namePath = UUID.randomUUID().toString()
                val reference = storageReference.child("images/$namePath")
                reference.putFile(uri).addOnSuccessListener {
                    storageReference.child("images/$namePath").downloadUrl.addOnSuccessListener { imageUrl ->
                        Glide.with(this)
                            .load(it)
                            .placeholder(R.drawable.ic_avatar)
                            .into(binding.imgAvatar)
                        CoroutineScope(Dispatchers.IO).launch {
                            conversationSid?.let { conversationSid ->
                                conversationsListManager.setAttributes(
                                    conversationSid = conversationSid,
                                    idSend = false,
                                    identity = credentialStorage.identity,
                                    deleted = messageViewModel.deletedAttribute.value
                                        ?: emptyList(),
                                    messageIndex = messageIndex,
                                    true,
                                    imageUrl.toString()
                                )
                            }

                        }
                        binding.progressBarUploadAvatar.isVisible = false


                    }.addOnFailureListener {
                        binding.progressBarUploadAvatar.isVisible = false
                        Toast.makeText(this, "Upload Failed!", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    binding.progressBarUploadAvatar.isVisible = false
                    Toast.makeText(this, "Upload Failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun checkPermissionCamera(): Boolean {
        return checkPermissions(
            arrayOf(Manifest.permission.CAMERA)
        )
    }


    private fun checkPermissions(permissions: Array<String>): Boolean {
        var check = true
        for (permission in permissions) {
            check = check and (PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(this, permission))
        }
        return check
    }

    private fun requestPermissionCamera() {
        val permission = arrayOf(
            Manifest.permission.CAMERA
        )
        ActivityCompat.requestPermissions(this, permission, CAMERA_PERMISSION)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION) {
            val cameraAndMicPermissionGranted =
                ((PackageManager.PERMISSION_GRANTED == grantResults[0]))
            if (cameraAndMicPermissionGranted) {
                startImageCapture()
            } else {
                requestPermissionCamera()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111 && resultCode == RESULT_OK) {
            if (data != null) {
                if (data.hasExtra(IMAGE_EXTRA_URI)) {
                    val uri = data.getStringExtra(IMAGE_EXTRA_URI)
                    uri?.let {
                        sendMediaMessage(it.toUri())
                    }
                }
            }
        }
    }

    private fun viewUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, null))
    }

    private fun shareUri(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, null))
    }

    @Suppress("UnusedEquals")
    private fun sendMessage() {
        binding.edtMessageInput.text.toString().takeIf { it.isNotBlank() }?.let { message ->
            messageViewModel.sendTextMessage(message)
            binding.edtMessageInput.text?.clear()
            val deleteAttribute = messageViewModel.deletedAttribute.value
            val avatarUrl = messageViewModel.imageAvatarUrl.value
            val list: MutableList<Deleted> = arrayListOf()
            list.clear()
            if (!deleteAttribute.isNullOrEmpty()) {
                deleteAttribute.forEach { deleted ->
                    list.add(Deleted(deleted.messageIndex, false, deleted.identity))
                }
            }
            if (!list.isNullOrEmpty()) {
                conversationSid?.let {
                    messageViewModel.setAttributeConversations(it, list, 0, avatarUrl ?: "")
                }
            }
        }
    }

    private fun resendMessage(message: MessageListViewItem) {
        if (message.type == MessageType.TEXT) {
            messageViewModel.resendTextMessage(message.uuid)
            val deleteAttribute = messageViewModel.deletedAttribute.value
            val avatarUrl = messageViewModel.imageAvatarUrl.value
            val list: MutableList<Deleted> = arrayListOf()
            list.clear()
            if (!deleteAttribute.isNullOrEmpty()) {
                deleteAttribute.forEach { deleted ->
                    list.add(Deleted(deleted.messageIndex, false, deleted.identity))
                }
            }
            if (!list.isNullOrEmpty()) {
                conversationSid?.let {
                    messageViewModel.setAttributeConversations(it, list, 0, avatarUrl ?: "")
                }
            }
        } else if (message.type == MessageType.MEDIA) {
            val fileInputStream =
                message.mediaUploadUri?.let { contentResolver.openInputStream(it) }
            if (fileInputStream != null) {
                messageViewModel.resendMediaMessage(fileInputStream, message.uuid)
                val deleteAttribute = messageViewModel.deletedAttribute.value
                val avatarUrl = messageViewModel.imageAvatarUrl.value
                val list: MutableList<Deleted> = arrayListOf()
                list.clear()
                if (!deleteAttribute.isNullOrEmpty()) {
                    deleteAttribute.forEach { deleted ->
                        list.add(Deleted(deleted.messageIndex, false, deleted.identity))
                    }
                }
                if (!list.isNullOrEmpty()) {
                    conversationSid?.let {
                        messageViewModel.setAttributeConversations(it, list, 0, avatarUrl ?: "")
                    }
                }
            } else {
                showToast(R.string.err_failed_to_resend_media)
            }
        }
    }

    private fun showRemoveMessageDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.remove_message_dialog_title)
            .setMessage(R.string.remove_message_dialog_message)
            .setPositiveButton(R.string.close, null)
            .setNegativeButton(R.string.delete) { _, _ -> messageViewModel.removeMessage() }
            .create()

        dialog.setOnShowListener {
            val color = ContextCompat.getColor(this, R.color.red)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
        }

        dialog.show()
    }

    private fun showSendErrorDialog(message: MessageListViewItem) {
        val title = getString(R.string.send_error_dialog_title, message.errorCode)

        val text = when (message.errorCode) { // See https://www.twilio.com/docs/api/errors
            50511 -> getString(R.string.send_error_dialog_invalid_media_content_type)
            else -> getString(R.string.send_error_dialog_message_default)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton(R.string.close, null)
            .setNegativeButton(R.string.retry) { _, _ -> resendMessage(message) }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
        }

        dialog.show()
    }

    private var imageCaptureUri = Uri.EMPTY

    private fun startImageCapture() {
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", picturesDir)
        imageCaptureUri =
            FileProvider.getUriForFile(
                this,
                "com.twilio.conversation.fileprovider",
                photoFile
            )

        takePicture.launch(imageCaptureUri)
    }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                CropImageActivity.start(this, imageCaptureUri)
            }
        }

    private fun sendMediaMessage(uri: Uri) {
        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val type = contentResolver.getType(uri)
        val name = contentResolver.getString(uri, OpenableColumns.DISPLAY_NAME)
        if (inputStream != null) {
            messageViewModel.sendMediaMessage(uri.toString(), inputStream, name, type)
            val deleteAttribute = messageViewModel.deletedAttribute.value
            val avatarUrl = messageViewModel.imageAvatarUrl.value
            val list: MutableList<Deleted> = arrayListOf()
            list.clear()
            if (!deleteAttribute.isNullOrEmpty()) {
                deleteAttribute.forEach { deleted ->
                    list.add(Deleted(deleted.messageIndex, false, deleted.identity))
                }
            }
            if (!list.isNullOrEmpty()) {
                conversationSid?.let {
                    messageViewModel.setAttributeConversations(
                        it, list, 0, avatarUrl ?: ""
                    )
                }
            }
        } else {
            messageViewModel.onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
        }
    }

    private fun showSortPopup(p: Point) {
        val dialogActionMessageBinding =
            ViewActionBinding.inflate(LayoutInflater.from(this))

        // Creating the PopupWindow
        changeSortPopUp.contentView = dialogActionMessageBinding.root
        changeSortPopUp.width = LinearLayout.LayoutParams.MATCH_PARENT
        changeSortPopUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        changeSortPopUp.isFocusable = false


        changeSortPopUp.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_action_message
            )
        )
        // Some offset to align the popup a bit to the left, and a bit down, relative to button's position.
        val OFFSET_X = -20
        val OFFSET_Y = 95
        // Displaying the popup at the specified location, + offsets.
        changeSortPopUp.showAtLocation(
            dialogActionMessageBinding.root,
            Gravity.NO_GRAVITY,
            p.x + OFFSET_X,
            p.y + OFFSET_Y
        )

        val message = messageViewModel.selectedMessage ?: run {
            changeSortPopUp.dismiss()
            return
        }
        val reactionsView = dialogActionMessageBinding.root

        reactionsView.reactions = message.reactions
        messageViewModel.selfUser.observe(this) {
            reactionsView.identity = it.identity
        }

        reactionsView.onChangeListener = {
            messageViewModel.setAttributes(
                reactionsView.reactions,
                message.deleteStatus.type,
                message.seenStatus
            )
            changeSortPopUp.dismiss()
            hideDialogAction()
        }


    }


    private fun showAction() {
        binding.viewHideAction.isVisible = true
        binding.layoutActionMessage.root.animate()
            .translationY(-(binding.layoutActionMessage.root.height.toFloat()))
            .setInterpolator(LinearInterpolator())
            .setStartDelay(200)
            .alpha(1f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                    binding.layoutActionMessage.root.isVisible = true
                }

                override fun onAnimationEnd(p0: Animator?) {
                }

                override fun onAnimationCancel(p0: Animator?) {
                }

                override fun onAnimationRepeat(p0: Animator?) {
                }

            })
            .start()
    }

    private fun hideDialogAction() {
        binding.viewHideAction.isVisible = false
        binding.layoutActionMessage.root.animate()
            .translationY(0f)
            .setInterpolator(LinearInterpolator())
            .setStartDelay(200)
            .alpha(0f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                }

                override fun onAnimationEnd(p0: Animator?) {
                    binding.layoutActionMessage.root.isVisible = false
                }

                override fun onAnimationCancel(p0: Animator?) {
                }

                override fun onAnimationRepeat(p0: Animator?) {
                }

            })
            .start()

    }

    private fun showMessageActionsDialog(reactions: Reactions, messageType: Int) {
        conversationSid?.let {
            MessageDeleteDialog(messageViewModel, reactions, messageType, seenStatus).showNow(
                supportFragmentManager,
                "MessageActionsDialog"
            )
        }

    }


}

