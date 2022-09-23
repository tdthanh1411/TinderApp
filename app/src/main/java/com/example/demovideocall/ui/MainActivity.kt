package com.example.demovideocall.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.demovideocall.R
import com.example.demovideocall.common.PagerAdapter
import com.example.demovideocall.databinding.ActivityMainBinding
import com.example.demovideocall.ui.call.CallFragment
import com.example.demovideocall.ui.conversation.ConversationsFragment
import com.example.demovideocall.ui.tinder.TinderHomeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@AndroidEntryPoint
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainActivity : AppCompatActivity() {
    companion object {
        const val CAMERA_MIC_PERMISSION_INDEX = 2022

        fun start(context: Context) {
            val intent = getStartIntent(context)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }

        fun getStartIntent(context: Context) =
            Intent(context, MainActivity::class.java)
    }


    private lateinit var binding: ActivityMainBinding

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var conversationsFragment: ConversationsFragment
    private lateinit var callFragment: CallFragment
    private lateinit var tinderHomeFragment: TinderHomeFragment

    private lateinit var pagerAdapter: PagerAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainViewModel.getToken()
        initViewPager()

    }


    private fun initViewPager() {
        tinderHomeFragment = TinderHomeFragment()
        conversationsFragment = ConversationsFragment()
        callFragment = CallFragment()

        pagerAdapter = PagerAdapter(this)
        pagerAdapter.addFragment(tinderHomeFragment)
        pagerAdapter.addFragment(conversationsFragment)
        pagerAdapter.addFragment(callFragment)

        with(binding) {
            viewPager.isUserInputEnabled = false
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    bottomNavigation.menu.getItem(position).isChecked = true
                }
            })
            viewPager.adapter = pagerAdapter

            bottomNavigation.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.tinder -> viewPager.currentItem = 0
                    R.id.conversation -> viewPager.currentItem = 1
                    R.id.call -> viewPager.currentItem = 2
                }
                return@setOnItemSelectedListener true
            }
        }


    }


    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        }

        ActivityCompat.requestPermissions(this, permission, CAMERA_MIC_PERMISSION_INDEX)

    }

    private fun checkPermissionForCameraAndMicrophone(): Boolean {
        return checkPermissions(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_MIC_PERMISSION_INDEX) {
            val cameraAndMicPermissionGranted =
                ((PackageManager.PERMISSION_GRANTED == grantResults[0]) and (PackageManager.PERMISSION_GRANTED == grantResults[1]))

            if (cameraAndMicPermissionGranted) {
//                updateUi()
            } else {
                requestPermission()
            }
        }

    }

    fun updateBadge(count: Int) {
        if (count <= 0) {
            binding.bottomNavigation.getOrCreateBadge(R.id.conversation).apply {
                backgroundColor = Color.RED
                badgeTextColor = Color.WHITE
                isVisible = false
            }
        } else {
            binding.bottomNavigation.getOrCreateBadge(R.id.conversation).apply {
                backgroundColor = Color.RED
                badgeTextColor = Color.WHITE
                number = count
                isVisible = true
            }
        }
    }


}