package io.atomofiron.outline

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.R as AndroidR
import android.os.Build.VERSION_CODES.TIRAMISU as AndroidT
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

// todo use ColorStateList

val isCurveUnavailable = SDK_INT < AndroidR
val isCurveWorks = SDK_INT >= AndroidT
var forceLegacy = false
val legacyMode get() = forceLegacy || isCurveUnavailable
//val legacyMode get() = SDK_INT < AndroidT


fun <V : View> V.rippleForeground(cornerRadius: Float): V = ripple(cornerRadius, toForeground = true)

fun <V : View> V.rippleBackground(cornerRadius: Float, backgroundColor: Int = Color.TRANSPARENT): V = ripple(cornerRadius, backgroundColor, toForeground = false)

private fun <V : View> V.ripple(cornerRadius: Float, contentColor: Int = Color.TRANSPARENT, toForeground: Boolean): V {
    val content = if (contentColor != 0) RoundCornersDrawable.fill(contentColor, cornerRadius) else null
    return ripple(cornerRadius, content, toForeground)
}

private fun <V : View> V.ripple(cornerRadius: Float, content: Drawable?, toForeground: Boolean): V {
    val rippleColor = androidx.appcompat.R.color.abc_color_highlight_material
            .let { ContextCompat.getColor(context, it) }
            .let { ColorStateList.valueOf(it) }
    val mask = RoundCornersDrawable.fill(Color.BLACK, cornerRadius)
    clipToOutline = true
    RippleDrawable(rippleColor, content, mask).let {
        if (toForeground) {
            foreground = it
            outlineProvider = it.getOutlineProvider()
        } else {
            background = it
        }
    }
    return this
}

fun <V : View> V.setRoundedBorder(
    borderColor: Int,
    cornerRadius: Float,
    borderWidth: Float = resources.displayMetrics.density,
): V {
    val drawable = RoundCornersDrawable.stroke(borderColor, cornerRadius, borderWidth)
    foreground = drawable
    outlineProvider = drawable.getOutlineProvider()
    clipToOutline = true
    return this
}

fun <V : View> V.clipToOutline(cornerRadius: Float): V {
    clipToOutline = true
    outlineProvider = when {
        legacyMode -> RoundRectOutlineProvider(cornerRadius)
        else -> CubicCornersOutlineProvider(cornerRadius)
    }
    return this
}

private fun Drawable.getOutlineProvider() = object : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        setBounds(0, 0, view.width, view.height)
        getOutline(outline)
    }
}

private class RoundRectOutlineProvider(private val radius: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, radius)
}

@RequiresApi(Build.VERSION_CODES.R)
private class CubicCornersOutlineProvider(private val radius: Float) : ViewOutlineProvider() {
    private val path = Path()
    private val rect = RectF()

    override fun getOutline(view: View, outline: Outline) {
        rect.right = view.width.toFloat()
        rect.bottom = view.height.toFloat()
        path.createRoundedCorners(rect, radius)
        outline.setPath(path)
    }
}

class RoundCornersDrawable private constructor(
    private val color: Int,
    private val radius: Float,
    private val style: Style,
    private val strokeWidth: Float = 0f,
) : Drawable() {
    companion object {
        fun fill(color: Int, radius: Float) = RoundCornersDrawable(color, radius, Style.FILL, strokeWidth = 0f)
        fun stroke(color: Int, radius: Float, strokeWidth: Float) = RoundCornersDrawable(color, radius, Style.STROKE, strokeWidth)
    }
    private val path = Path()
    private val rect = RectF()
    private val paint = Paint().apply {
        isAntiAlias = true
        color = this@RoundCornersDrawable.color
        style = this@RoundCornersDrawable.style
        // clip the line in the middle during drawing
        strokeWidth = this@RoundCornersDrawable.strokeWidth * 2
    }

    override fun getOutline(outline: Outline) = when {
        legacyMode -> outline.setRoundRect(bounds, radius)
        else -> outline.setPath(path)
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        val updatePath = bounds.left != left || bounds.top != top || bounds.right != right || bounds.bottom != bottom
        super.setBounds(left, top, right, bottom)
        if (updatePath) {
            rect.set(bounds)
            //if (legacyMode) rect.inset(strokeWidth / 2, strokeWidth / 2)
            path.createRoundedCorners(rect, radius)
        }
    }

    override fun draw(canvas: Canvas) {
        if (legacyMode) {
            canvas.drawRoundRect(rect, radius, radius, paint)
        } else {
            canvas.clipPath(path)
            canvas.drawPath(path, paint)
        }
    }

    override fun setAlpha(alpha: Int) = paint.setAlpha(alpha)

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    fun getOutlineProvider() = (this as Drawable).getOutlineProvider()
}


private const val RADIUS_MULTIPLIER = 1.2f
private const val ZERO = 0f
private const val FULL = 1f
private const val LONG_PART = 0.67f
private const val SHORT_PART = FULL - LONG_PART

private class Corner(
    val dx1: Float,
    val dy1: Float,
    val dx2: Float,
    val dy2: Float,
    val dx: Float,
    val dy: Float,
    // to the next corner
    val lineX: Int,
    val lineY: Int,
)

// the starting point is at the bottom of the top left corner
private val corners = arrayOf(
    Corner(ZERO, -LONG_PART, SHORT_PART, -FULL, FULL, -FULL, lineX = 1, lineY = 0),
    Corner(LONG_PART, ZERO, FULL, SHORT_PART, FULL, FULL, lineX = 0, lineY = 1),
    Corner(ZERO, LONG_PART, -SHORT_PART, FULL, -FULL, FULL, lineX = -1, lineY = 0),
    Corner(-LONG_PART, ZERO, -FULL, -SHORT_PART, -FULL, -FULL, lineX = 0, lineY = -1),
)

fun Path.createRoundedCorners(bounds: RectF, radius: Float) {
    reset()
    appendRoundedCorners(bounds, radius)
    close()
}

fun Path.appendRoundedCorners(bounds: RectF, radius: Float) {
    val offset = radius * RADIUS_MULTIPLIER
    val straightX = bounds.width() - offset * 2
    val straightY = bounds.height() - offset * 2
    // todo check bound limits
    moveTo(bounds.left, bounds.top + offset)
    corners.forEach {
        rCubicTo(offset * it.dx1, offset * it.dy1, offset * it.dx2, offset * it.dy2, offset * it.dx, offset * it.dy)
        rLineTo(straightX * it.lineX, straightY * it.lineY)
    }
}

fun generateRoundedCornersPathData(bounds: RectF, radius: Float): String {
    val offset = radius * RADIUS_MULTIPLIER
    val straightX = bounds.width() - offset * 2
    val straightY = bounds.height() - offset * 2
    val builder = StringBuilder()
    builder.append("M ${bounds.left} ${bounds.top + offset} ")
    corners.forEach {
        builder.append("c ${offset * it.dx1} ${offset * it.dy1} ${offset * it.dx2} ${offset * it.dy2} ${offset * it.dx} ${offset * it.dy} ")
        builder.append("l ${straightX * it.lineX} ${straightY * it.lineY} ")
    }
    builder.append("z")
    return builder.toString()
}
