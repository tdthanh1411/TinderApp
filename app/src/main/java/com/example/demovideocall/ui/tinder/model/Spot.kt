package com.example.demovideocall.ui.tinder.model

data class Spot(
    val id: Long = counter++,
    val name: String,
    val city: String,
    val imageData: List<ImageData>
) {
    companion object {
        private var counter = 0L
    }
}
