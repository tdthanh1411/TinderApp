package com.example.demovideocall.ui.tinder

import androidx.recyclerview.widget.DiffUtil
import com.example.demovideocall.ui.tinder.model.Spot

class SpotDiffCallback(
    private val old: List<Spot>,
    private val new: List<Spot>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return old.size
    }

    override fun getNewListSize(): Int {
        return new.size
    }

    override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return old[oldPosition].id == new[newPosition].id
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return old[oldPosition] == new[newPosition]
    }

}
