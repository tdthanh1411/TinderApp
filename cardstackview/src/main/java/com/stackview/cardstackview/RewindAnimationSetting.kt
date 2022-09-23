package com.stackview.cardstackview

import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import com.stackview.cardstackview.internal.AnimationSetting
import com.stackview.cardstackview.RewindAnimationSetting

class RewindAnimationSetting private constructor(
    override val direction: Direction,
    override val duration: Int,
    override val interpolator: Interpolator
) : AnimationSetting {

    class Builder {
        private var direction = Direction.Bottom
        private var duration = Duration.Normal.duration
        private var interpolator: Interpolator = DecelerateInterpolator()
        fun setDirection(direction: Direction): Builder {
            this.direction = direction
            return this
        }

        fun setDuration(duration: Int): Builder {
            this.duration = duration
            return this
        }

        fun setInterpolator(interpolator: Interpolator): Builder {
            this.interpolator = interpolator
            return this
        }

        fun build(): RewindAnimationSetting {
            return RewindAnimationSetting(
                direction,
                duration,
                interpolator
            )
        }
    }
}