package com.example.demovideocall.common

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Created by ThanhTran on 7/20/2022.
 */


class PagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    private var fragment: MutableList<Fragment> = mutableListOf()
    fun addFragment(fragment: Fragment) {
        this.fragment.add(fragment)
    }

    override fun getItemCount(): Int {
        return fragment.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragment[position]
    }
}