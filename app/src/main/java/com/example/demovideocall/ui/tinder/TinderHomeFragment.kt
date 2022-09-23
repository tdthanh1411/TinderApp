package com.example.demovideocall.ui.tinder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import com.example.demovideocall.R
import com.example.demovideocall.common.extensions.pulse
import com.example.demovideocall.databinding.FragmentTinderHomeBinding
import com.example.demovideocall.ui.MainActivity
import com.example.demovideocall.ui.tinder.model.ImageData
import com.example.demovideocall.ui.tinder.model.Spot
import com.stackview.cardstackview.*

class TinderHomeFragment : Fragment(), CardStackListener {

    private lateinit var _binding: FragmentTinderHomeBinding
    private val binding get() = _binding

    private val activity by lazy { requireActivity() as MainActivity }

    private val manager by lazy { CardStackLayoutManager(activity, this) }
    private val cardStackAdapter by lazy { CardStackAdapter(createSpots()) }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTinderHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardStackView()
        initListener()
    }

    private fun initListener() {
        with(binding) {
            imgRefresh.setOnClickListener {

            }
            imgSkip.setOnClickListener {
                it.pulse()
                val setting = SwipeAnimationSetting.Builder()
                    .setDirection(Direction.Left)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(AccelerateInterpolator())
                    .build()
                manager.setSwipeAnimationSetting(setting)
                cardStackView.swipe()
            }
            imgStart.setOnClickListener {

            }
            imgFavorite.setOnClickListener {
                it.pulse()
                val setting = SwipeAnimationSetting.Builder()
                    .setDirection(Direction.Right)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(AccelerateInterpolator())
                    .build()
                manager.setSwipeAnimationSetting(setting)
                cardStackView.swipe()
            }
            imgBolt.setOnClickListener {

            }
        }

    }


    private fun setupCardStackView() {
        initialize()
    }

    private fun initialize() {
        manager.setStackFrom(StackFrom.None)
        manager.setVisibleCount(3)
        manager.setTranslationInterval(8.0f)
        manager.setScaleInterval(0.95f)
        manager.setSwipeThreshold(0.3f)
        manager.setMaxDegree(20.0f)
        manager.setDirections(Direction.HORIZONTAL)
        manager.setCanScrollHorizontal(true)
        manager.setCanScrollVertical(true)
        manager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
        manager.setOverlayInterpolator(LinearInterpolator())
        with(binding.cardStackView) {
            apply {
                layoutManager = manager
                adapter = cardStackAdapter
                itemAnimator.apply {
                    if (this is DefaultItemAnimator) {
                        supportsChangeAnimations = false
                    }
                }
            }
        }
    }


    override fun onCardDragging(direction: Direction, ratio: Float) {
        Log.d("CardStackView", "onCardDragging: d = ${direction.name}, r = $ratio")
    }

    override fun onCardSwiped(direction: Direction) {
        Log.d(
            "CardStackView",
            "onCardSwiped: p = ${manager.topPosition}, d = $direction,----- ${manager.topPosition}"
        )
        if (manager.topPosition == cardStackAdapter.itemCount - 5) {
            paginate()
        }
    }

    override fun onCardRewound() {
        Log.d("CardStackView", "onCardRewound: ${manager.topPosition}")
    }

    override fun onCardCanceled() {
        Log.d("CardStackView", "onCardCanceled: ${manager.topPosition}")
    }

    override fun onCardAppeared(view: View, position: Int) {
        val textView = view.findViewById<TextView>(R.id.tvName)
        Log.d("CardStackView", "onCardAppeared: ($position) ${textView.text}")
    }

    override fun onCardDisappeared(view: View, position: Int) {
        val textView = view.findViewById<TextView>(R.id.tvCity)
        Log.d(
            "CardStackView",
            "onCardDisappeared: ($position) ${textView.text} ------- ${
                cardStackAdapter.getData(position)
            }"
        )
    }

    private fun paginate() {
        val old = cardStackAdapter.getSpots()
        val new = old.plus(createSpots())
        val callback = SpotDiffCallback(old, new)
        val result = DiffUtil.calculateDiff(callback)
        cardStackAdapter.setSpots(new)
        result.dispatchUpdatesTo(cardStackAdapter)
    }

    private fun createSpots(): List<Spot> {
        val spots = ArrayList<Spot>()
        spots.add(
            Spot(
                name = "Yasaka Shrine",
                city = "Kyoto",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/Xq1ntWruZQI/600x800"),
                    ImageData("https://source.unsplash.com/NYyCqdBOKwc/600x800"),
                    ImageData("https://source.unsplash.com/Xq1ntWruZQI/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "Fushimi Inari Shrine",
                city = "Kyoto",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/NYyCqdBOKwc/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "Yasaka Shrine",
                city = "Kyoto",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/Xq1ntWruZQI/600x800"),
                    ImageData("https://source.unsplash.com/NYyCqdBOKwc/600x800"),
                    ImageData("https://source.unsplash.com/NYyCqdBOKwc/600x800"),
                    ImageData("https://source.unsplash.com/Xq1ntWruZQI/600x800"),
                    ImageData("https://source.unsplash.com/NYyCqdBOKwc/600x800"),
                    ImageData("https://source.unsplash.com/NYyCqdBOKwc/600x800"),
                    ImageData("https://source.unsplash.com/Xq1ntWruZQI/600x800"),
                    ImageData("https://source.unsplash.com/NYyCqdBOKwc/600x800"),
                    ImageData("https://source.unsplash.com/NYyCqdBOKwc/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "Bamboo Forest",
                city = "Kyoto",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/buF62ewDLcQ/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "Brooklyn Bridge",
                city = "New York",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/THozNzxEP3g/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "Empire State Building",
                city = "New York",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/USrZRcRS2Lw/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "The statue of Liberty",
                city = "New York",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/PeFk7fzxTdk/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "Louvre Museum",
                city = "Paris",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/LrMWHKqilUw/600x800"),
                    ImageData("https://source.unsplash.com/HN-5Z6AmxrM/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "Eiffel Tower",
                city = "Paris",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/HN-5Z6AmxrM/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "Big Ben",
                city = "London",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/CdVAUADdqEc/600x800")
                )
            )
        )
        spots.add(
            Spot(
                name = "Great Wall of China",
                city = "China",
                imageData = listOf(
                    ImageData("https://source.unsplash.com/AWh9C-QjhE4/600x800")
                )
            )
        )
        return spots
    }


}