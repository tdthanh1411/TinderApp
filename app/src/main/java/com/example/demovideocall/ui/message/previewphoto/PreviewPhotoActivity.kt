package com.example.demovideocall.ui.message.previewphoto

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.example.demovideocall.databinding.ActivityPreviewPhotoBinding
import com.twilio.conversation.R
import com.twilio.conversation.ui.message.crop.CropImage
import com.twilio.conversation.ui.message.crop.CropImageActivity

class PreviewPhotoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreviewPhotoBinding

    companion object {
        private const val EXTRA_PHOTO_URL = "EXTRA_PHOTO_URL"
        fun start(activity: Activity, url: String) {
            val intent = Intent(activity, PreviewPhotoActivity::class.java)
            intent.putExtra(EXTRA_PHOTO_URL, url)
            ActivityCompat.startActivity(
                activity,
                intent,
                null
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPhoto()
        binding.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun initPhoto() {
        val url = intent.getStringExtra(EXTRA_PHOTO_URL)
        url?.let {
            Glide.with(this)
                .asBitmap()
                .placeholder(R.drawable.placeholder)
                .load(it)
                .into(object :
                    BitmapImageViewTarget(binding.touchImageView) {
                    override fun setResource(resource: Bitmap?) {
                        super.setResource(resource)
                    }
                })
        }
    }
}