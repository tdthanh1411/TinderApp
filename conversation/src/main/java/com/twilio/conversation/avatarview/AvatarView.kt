@file:Suppress("MemberVisibilityCanBePrivate")

package com.twilio.conversation.avatarview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import com.twilio.conversation.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr), CoroutineScope {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val indicatorOutlinePaint = Paint().apply { style = Paint.Style.FILL }
    private val indicatorPaint = Paint().apply { style = Paint.Style.FILL }
    private val backgroundPaint = Paint().apply { style = Paint.Style.FILL }
    private val textPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }

    /** The border width of AvatarView. */
    @get:Px
    var avatarBorderWidth: Int by viewProperty(3.dp)

    /** The border color of AvatarView. */
    @get:ColorInt
    var avatarBorderColor: Int by viewProperty(Color.WHITE)

    /** The border color array of AvatarView. */
    var avatarBorderColorArray: IntArray by viewProperty(intArrayOf())

    /** The border radius of AvatarView. */
    @get:Px
    var avatarBorderRadius: Float by viewProperty(6.dp.toFloat())

    /** The shape of the AvatarView. */
    var avatarShape: AvatarShape by viewProperty(AvatarShape.CIRCLE)

    /** The initials to be drawn instead of an image. */
    var avatarInitials: String? by viewProperty(null)

    /** The text size of the initials. */
    @get:Px
    var avatarInitialsTextSize: Int by viewProperty(-1)

    /** The text size ratio of the initials. */
    @get:FloatRange(from = 0.0, to = 1.0)
    var avatarInitialsTextSizeRatio: Float by viewProperty(0.33f)

    /** The text color of the initials. */
    @get:ColorInt
    var avatarInitialsTextColor: Int by viewProperty(Color.WHITE)

    /** The text styles color of the initials. */
    var avatarInitialsStyle: Int by viewProperty(Typeface.NORMAL)

    /** The background color of the initials. */
    @get:ColorInt
    var avatarInitialsBackgroundColor: Int by viewProperty(internalBlue)

    /** Sets the visibility of the indicator. */
    var indicatorEnabled: Boolean by viewProperty(false)

    /** The position of the indicator. */
    var indicatorPosition: IndicatorPosition by viewProperty(IndicatorPosition.TOP_RIGHT)

    /** The color of the indicator. */
    @get:ColorInt
    var indicatorColor: Int by viewProperty(internalGreen)

    /** The border color of the indicator. */
    @get:ColorInt
    var indicatorBorderColor: Int by viewProperty(Color.WHITE)

    /** The border color array of the indicator. */
    var indicatorBorderColorArray: IntArray by viewProperty(intArrayOf())

    /** The size criteria of the indicator. */
    var indicatorSizeCriteria: Float by viewProperty(8f)

    /** The border size criteria of the indicator. This must be bigger than the [indicatorSizeCriteria]. */
    var indicatorBorderSizeCriteria: Float by viewProperty(10f)

    /** A custom indicator view. */
    var indicatorDrawable: Drawable? by viewProperty(null)

    /** Supports RTL layout is enabled or not. */
    var supportRtlEnabled: Boolean by viewProperty(true)

    /** A placeholder that should be shown when loading an image. */
    var placeholder: Drawable? by viewProperty(null)

    /** An error placeholder that should be shown when request failed. */
    var errorPlaceholder: Drawable? by viewProperty(null)

    /**
     * The maximum section size of the avatar when loading multiple images.
     * This size must between 1 and 4.
     */
    var maxSectionSize: Int = 4
        set(value) {
            field = value.coerceIn(1..4)
        }

    /** Internal coroutine scope for AvatarView. */
    @property:InternalAvatarViewApi
    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Main

    init {
        initAttributes(attrs, defStyleAttr)
    }

    private fun initAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs ?: return
        context.obtainStyledAttributes(attrs, R.styleable.AvatarView, defStyleAttr, 0)
            .use { typedArray ->
                avatarBorderWidth = typedArray.getDimensionPixelSize(
                    R.styleable.AvatarView_avatarViewBorderWidth, avatarBorderWidth
                )
                avatarBorderColor = typedArray.getColor(
                    R.styleable.AvatarView_avatarViewBorderColor, avatarBorderColor
                )
                avatarBorderColorArray = typedArray.getIntArray(
                    R.styleable.AvatarView_avatarViewBorderColorArray, intArrayOf()
                )
                avatarBorderRadius = typedArray.getDimension(
                    R.styleable.AvatarView_avatarViewBorderRadius,
                    avatarBorderRadius
                )
                avatarInitials = typedArray.getString(
                    R.styleable.AvatarView_avatarViewInitials
                )
                avatarInitialsTextSize = typedArray.getDimensionPixelSize(
                    R.styleable.AvatarView_avatarViewInitialsTextSize,
                    avatarInitialsTextSize
                )
                avatarInitialsTextSizeRatio = typedArray.getFloat(
                    R.styleable.AvatarView_avatarViewInitialsTextSizeRatio,
                    avatarInitialsTextSizeRatio
                )
                avatarInitialsTextColor = typedArray.getColor(
                    R.styleable.AvatarView_avatarViewInitialsTextColor,
                    avatarInitialsTextColor
                )
                avatarInitialsBackgroundColor = typedArray.getColor(
                    R.styleable.AvatarView_avatarViewInitialsBackgroundColor,
                    avatarInitialsBackgroundColor
                )
                avatarInitialsStyle = typedArray.getInt(
                    R.styleable.AvatarView_avatarViewInitialsTextStyle,
                    avatarInitialsStyle
                )
                avatarShape = typedArray.getEnum(
                    R.styleable.AvatarView_avatarViewShape,
                    avatarShape
                )
                indicatorEnabled = typedArray.getBoolean(
                    R.styleable.AvatarView_avatarViewIndicatorEnabled,
                    indicatorEnabled
                )
                indicatorPosition = typedArray.getEnum(
                    R.styleable.AvatarView_avatarViewIndicatorPosition,
                    indicatorPosition
                )
                indicatorColor = typedArray.getColor(
                    R.styleable.AvatarView_avatarViewIndicatorColor, indicatorColor
                )
                indicatorBorderColor = typedArray.getColor(
                    R.styleable.AvatarView_avatarViewIndicatorBorderColor,
                    indicatorBorderColor
                )
                indicatorBorderColorArray = typedArray.getIntArray(
                    R.styleable.AvatarView_avatarViewIndicatorBorderColorArray,
                    indicatorBorderColorArray
                )
                indicatorSizeCriteria = typedArray.getFloat(
                    R.styleable.AvatarView_avatarViewIndicatorSizeCriteria,
                    indicatorSizeCriteria
                )
                indicatorBorderSizeCriteria = typedArray.getFloat(
                    R.styleable.AvatarView_avatarViewIndicatorBorderSizeCriteria,
                    indicatorBorderSizeCriteria
                )
                indicatorDrawable = typedArray.getDrawable(
                    R.styleable.AvatarView_avatarViewIndicatorDrawable
                )
                supportRtlEnabled = typedArray.getBoolean(
                    R.styleable.AvatarView_avatarViewSupportRtlEnabled,
                    supportRtlEnabled
                )
                maxSectionSize = typedArray.getInt(
                    R.styleable.AvatarView_avatarViewMaxSectionSize,
                    maxSectionSize
                )
                placeholder = typedArray.getDrawable(
                    R.styleable.AvatarView_avatarViewPlaceholder
                )
                errorPlaceholder = typedArray.getDrawable(
                    R.styleable.AvatarView_avatarViewErrorPlaceholder
                )
            }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(0, widthMeasureSpec)
        val height = resolveSize(0, heightMeasureSpec)
        val avatarViewSize = width.coerceAtMost(height)
        setMeasuredDimension(avatarViewSize, avatarViewSize)
    }

    override fun onDraw(canvas: Canvas) {
        if (avatarInitials.isNullOrEmpty()) {
            if (drawable != null) {
                super.onDraw(canvas)
                applyPaintStyles()
                drawBorder(canvas)
                drawIndicator(canvas)
            }
        } else {
            applyPaintStyles()
            drawColor(canvas)
            drawInitials(canvas)
            drawBorder(canvas)
            drawIndicator(canvas)
        }
    }

    /** Applies custom attributes to [AvatarView]. */
    private fun applyPaintStyles() {
        borderPaint.color = avatarBorderColor
        borderPaint.strokeWidth = avatarBorderWidth.toFloat()
        val padding = (avatarBorderWidth - AVATAR_SIZE_EXTRA).coerceAtLeast(0)
        setPadding(padding, padding, padding, padding)
        indicatorOutlinePaint.color = indicatorBorderColor
        indicatorPaint.color = indicatorColor
        backgroundPaint.color = avatarInitialsBackgroundColor
        textPaint.color = avatarInitialsTextColor
        textPaint.typeface = Typeface.defaultFromStyle(avatarInitialsStyle)
        textPaint.textSize = avatarInitialsTextSize.takeIf { it != -1 }?.toFloat()
            ?: (avatarInitialsTextSizeRatio * width)
    }

    /** Draws a border to [AvatarView]. */
    private fun drawBorder(canvas: Canvas) {
        if (avatarBorderWidth == 0) return

        if (avatarShape == AvatarShape.ROUNDED_RECT) {
            canvas.drawRoundRect(
                BORDER_OFFSET,
                BORDER_OFFSET,
                width.toFloat() - BORDER_OFFSET,
                height.toFloat() - BORDER_OFFSET,
                avatarBorderRadius,
                avatarBorderRadius,
                borderPaint.applyGradientShader(avatarBorderColorArray, width / 2f, height / 2f)
            )
        } else {
            canvas.drawCircle(
                width / 2f,
                height / 2f,
                width / 2f - avatarBorderWidth / 2,
                borderPaint.applyGradientShader(avatarBorderColorArray, width / 2f, height / 2f)
            )
        }
    }

    /** Draws initials to [AvatarView]. */
    private fun drawInitials(canvas: Canvas) {
        val initials = avatarInitials ?: return
        canvas.drawText(
            initials.parseInitials,
            width.toFloat() / 2,
            (canvas.height / 2) - ((textPaint.descent() + textPaint.ascent()) / 2),
            textPaint
        )
    }

    /** Draws color of the initials to [AvatarView]. */
    private fun drawColor(canvas: Canvas) {
        if (avatarShape == AvatarShape.ROUNDED_RECT) {
            canvas.drawRoundRect(
                0F,
                0F,
                width.toFloat(),
                height.toFloat(),
                avatarBorderRadius,
                avatarBorderRadius,
                backgroundPaint
            )
        } else {
            canvas.drawCircle(
                width / 2f,
                height / 2f,
                width / 2f,
                backgroundPaint
            )
        }
    }

    /** Draws an indicator to [AvatarView]. */
    private fun drawIndicator(canvas: Canvas) {
        if (indicatorEnabled) {
            val isRtlEnabled = supportRtlEnabled && isRtlLayout

            val customIndicator = indicatorDrawable
            if (customIndicator != null) with(customIndicator) {
                val cx: Float = when (indicatorPosition) {
                    IndicatorPosition.TOP_LEFT,
                    IndicatorPosition.BOTTOM_LEFT,
                    -> if (isRtlEnabled) width - (width / indicatorSizeCriteria)
                    else 0f
                    IndicatorPosition.TOP_RIGHT,
                    IndicatorPosition.BOTTOM_RIGHT,
                    -> if (isRtlEnabled) width / indicatorSizeCriteria
                    else width - (width / indicatorSizeCriteria)
                }
                val cy: Float = when (indicatorPosition) {
                    IndicatorPosition.TOP_LEFT,
                    IndicatorPosition.TOP_RIGHT,
                    -> 0f
                    IndicatorPosition.BOTTOM_LEFT,
                    IndicatorPosition.BOTTOM_RIGHT,
                    -> height - height / indicatorSizeCriteria
                }
                setBounds(
                    cx.toInt(),
                    cy.toInt(),
                    (cx + width / indicatorSizeCriteria).toInt(),
                    (cy + height / indicatorSizeCriteria).toInt()
                )
                draw(canvas)
            } else {
                val cx: Float = when (indicatorPosition) {
                    IndicatorPosition.TOP_LEFT,
                    IndicatorPosition.BOTTOM_LEFT,
                    -> if (isRtlEnabled) width - (width / indicatorSizeCriteria)
                    else width / indicatorSizeCriteria
                    IndicatorPosition.TOP_RIGHT,
                    IndicatorPosition.BOTTOM_RIGHT,
                    -> if (isRtlEnabled) width / indicatorSizeCriteria
                    else width - (width / indicatorSizeCriteria)
                }

                val cy: Float = when (indicatorPosition) {
                    IndicatorPosition.TOP_LEFT,
                    IndicatorPosition.TOP_RIGHT,
                    -> height / indicatorSizeCriteria
                    IndicatorPosition.BOTTOM_LEFT,
                    IndicatorPosition.BOTTOM_RIGHT,
                    -> height - height / indicatorSizeCriteria
                }
                canvas.drawCircle(
                    cx,
                    cy,
                    width / indicatorSizeCriteria,
                    indicatorOutlinePaint.applyGradientShader(indicatorBorderColorArray, cx, cy)
                )
                canvas.drawCircle(cx, cy, width / indicatorBorderSizeCriteria, indicatorPaint)
            }
        }
    }

    /** Apply gradient shader to a [Paint]. */
    private fun Paint.applyGradientShader(colorArray: IntArray, cx: Float, cy: Float): Paint =
        apply {
            if (colorArray.isNotEmpty()) {
                shader = SweepGradient(
                    cx,
                    cy,
                    colorArray,
                    colorArray.arrayPositions
                )
            }
        }

    fun setIndicatorRes(@DrawableRes drawableRes: Int) {
        indicatorDrawable = ResourcesCompat.getDrawable(resources, drawableRes, null)
    }

    @OptIn(InternalAvatarViewApi::class)
    override fun onDetachedFromWindow() {
        coroutineContext.cancel()
        super.onDetachedFromWindow()
    }

    internal companion object {

        internal const val AVATAR_SIZE_EXTRA = 1

        private const val BORDER_OFFSET = 4F
    }
}