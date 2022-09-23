package com.example.demovideocall.ui.tinder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.demovideocall.common.extensions.pulse
import com.example.demovideocall.common.extensions.pulseOnlyUp
import com.example.demovideocall.common.extensions.scale
import com.example.demovideocall.databinding.ItemSpotBinding
import com.example.demovideocall.ui.tinder.model.ImageData
import com.example.demovideocall.ui.tinder.model.Spot
import com.google.android.material.tabs.TabLayoutMediator

class CardStackAdapter(
    private var spots: List<Spot> = emptyList()
) : RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSpotBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val spot = spots[position]

        with(holder.binding) {
            tvName.text = spot.name
            tvCity.text = spot.city

            setupViewPageImage(holder.binding, spot.imageData)

            nextViewPage.setOnClickListener {
                if (viewPager.currentItem < tabLayout.tabCount - 1) {
                    viewPager.setCurrentItem(viewPager.currentItem + 1, true)
                } else {
                    holder.binding.root.pulseOnlyUp()
                }
            }

            previewViewPage.setOnClickListener {
                if (viewPager.currentItem > 0) {
                    viewPager.setCurrentItem(viewPager.currentItem - 1, true)
                } else {
                    holder.binding.root.pulseOnlyUp()
                }
            }


        }

    }

    private fun setupViewPageImage(binding: ItemSpotBinding, imageData: List<ImageData>) {
        with(binding) {
            val imageAdapter = ImageAdapter(imageData)
            viewPager.adapter = imageAdapter
            tabLayout.isVisible = imageData.size > 1
            TabLayoutMediator(tabLayout, viewPager) { tab, _ ->
                viewPager.setCurrentItem(tab.position, true)
            }.attach()

            for (i in 0 until tabLayout.tabCount - 1) {
                val tab = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
                val p = tab.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(10, 0, 10, 0)
                tab.requestLayout()
            }
        }

    }

    override fun getItemCount(): Int {
        return spots.size
    }

    fun getData(position: Int): Spot {
        return spots[position]
    }

    fun setSpots(spots: List<Spot>) {
        this.spots = spots
    }

    fun getSpots(): List<Spot> {
        return spots
    }

    class ViewHolder(val binding: ItemSpotBinding) : RecyclerView.ViewHolder(binding.root)

}
