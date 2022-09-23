package com.twilio.conversation.ui.message.reaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.twilio.conversation.R
import com.twilio.conversation.common.enums.Reactions
import com.twilio.conversation.common.enums.TypeDeleteMessage
import com.twilio.conversation.databinding.DialogActionDeleteMessageBinding
import com.twilio.conversation.ui.message.MessageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MessageDeleteDialog(
    private val messageViewModel: MessageViewModel,
    private val reactions: Reactions,
    private val messageType: Int,
    private val seenStatus: String
) : BottomSheetDialogFragment() {

    lateinit var binding: DialogActionDeleteMessageBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogActionDeleteMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()


        with(binding) {
            tvCancel.setOnClickListener {
                dismiss()
            }
            tvUnseenEveryone.setOnClickListener {
                messageViewModel.setAttributes(
                    reactions = reactions,
                    TypeDeleteMessage.TYPE_DELETE_ALL.type,
                    seenStatus = seenStatus
                )
                dismiss()
            }
            tvUnseenForYou.setOnClickListener {
                messageViewModel.setAttributes(
                    reactions = reactions,
                    resources.getString(R.string.type_delete_for_you),
                    seenStatus = seenStatus
                )
                dismiss()
            }
        }
    }

    private fun initView() {
        binding.groupEveryOne.isVisible = messageType == 1
    }

}