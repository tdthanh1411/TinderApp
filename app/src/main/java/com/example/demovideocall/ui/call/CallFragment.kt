package com.example.demovideocall.ui.call

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.demovideocall.R
import com.example.demovideocall.databinding.FragmentCallBinding
import com.example.demovideocall.ui.room.RoomActivity

class CallFragment : Fragment() {
    private lateinit var _binding: FragmentCallBinding
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVideoCall.setOnClickListener {
            updateUi()
        }
    }

    private fun updateUi() {
        val intent = Intent(activity, RoomActivity::class.java)
        val name = binding.txtUserIdentity.text.toString();
        intent.putExtra("identity", name);
        startActivity(intent)
    }
}