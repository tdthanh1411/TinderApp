package com.example.demovideocall.ui.tinder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.demovideocall.databinding.ItemImageBinding
import com.example.demovideocall.ui.tinder.model.ImageData

class ImageAdapter(private val listImage: List<ImageData> = emptyList()) :
    RecyclerView.Adapter<ImageAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)


    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageData = listImage[position]

        Glide.with(holder.binding.imgThumbnail)
            .load(imageData.imgUrl)
            .into(holder.binding.imgThumbnail)
    }

    override fun getItemCount(): Int {
        return listImage.size
    }
}