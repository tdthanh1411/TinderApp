package com.twilio.conversation.ui.message.crop

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.twilio.conversation.databinding.ActivityCropBinding
import com.twilio.conversation.utils.ExtraUtils.IMAGE_EXTRA_URI

class CropImageActivity : AppCompatActivity(),
    CropImageView.OnCropImageCompleteListener {

    private lateinit var binding: ActivityCropBinding

    private var cropImageUri: Uri? = null


    companion object {
        fun start(activity: Activity, uri: Uri) {
            val bundle = Bundle()
            bundle.putParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE, uri)
            val intent = Intent(activity, CropImageActivity::class.java)
            intent.putExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE, bundle)
            ActivityCompat.startActivityForResult(
                activity,
                intent,
                111,
                null
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
        cropImageUri = bundle?.getParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)

        with(binding) {
            cropImageView.let {
                it.setOnCropImageCompleteListener(this@CropImageActivity)
            }

            cropImageView.setImageUriAsync(cropImageUri)

            tvCrop.setOnClickListener {
                cropImageView.croppedImageAsync()
            }
            imgBack.setOnClickListener {
                onBackPressed()
            }
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        val intent = Intent()
        intent.putExtra(IMAGE_EXTRA_URI, result.uriContent.toString())
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onStop() {
        super.onStop()
        with(binding) {
            cropImageView.setOnCropImageCompleteListener(null)
        }
    }
}