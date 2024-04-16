package demo.atomofiron.outline

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Paint.Join
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
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.R
import androidx.core.content.ContextCompat


val isCurveUnavailable = SDK_INT < AndroidR
val isCurveWorks = SDK_INT >= AndroidT
var forceLegacy = false
val legacyMode get() = forceLegacy || isCurveUnavailable
//val legacyMode get() = SDK_INT < AndroidT


data class StateType(
    val enabled: Boolean? = null,
    val checked: Boolean? = null,
    val activated: Boolean? = null,
    val selected: Boolean? = null,
) {
    companion object {
        val Undefined = StateType()
    }
}

sealed interface ColorType {
    companion object {
        fun transparent(state: StateType = StateType.Undefined) = Value(Color.TRANSPARENT, state)
        fun mask(state: StateType = StateType.Undefined) = Value(Color.BLACK, state)
    }
    data class Res(
        @ColorRes val colorId: Int,
        val state: StateType = StateType.Undefined,
    ) : ColorType
    data class Attr(
        @AttrRes val colorAttr: Int,
        val state: StateType = StateType.Undefined,
    ) : ColorType
    data class Value(
        @ColorInt val color: Int,
        val state: StateType = StateType.Undefined,
    ) : ColorType
}

sealed interface DimensionType {
    companion object {
        val Zero = Value(0)
    }
    data class Res(@DimenRes val dimenId: Int) : DimensionType
    data class Value(val value: Float) : DimensionType {
        constructor(value: Int) : this(value.toFloat())
    }
}

sealed interface ShapeStyle {
    data object Fill : ShapeStyle
    data class Stroke(
        val width: DimensionType,
        val cap: Cap,
        val join: Join,
    ) : ShapeStyle {
        constructor(
            @DimenRes width: Int,
            cap: Cap = Cap.SQUARE,
            join: Join = Join.MITER,
        ) : this(DimensionType.Res(width), cap, join)
        constructor(
            width: Float = 1f,
            cap: Cap = Cap.SQUARE,
            join: Join = Join.MITER,
        ) : this(DimensionType.Value(width), cap, join)
    }
}

sealed interface RadiusType {
    companion object {
        operator fun invoke(radius: Float) = Single(radius)
        operator fun invoke(@DimenRes dimenId: Int) = Single(dimenId)

        fun left(radius: Float) = Radii.Zero.copy(topLeft = DimensionType.Value(radius), bottomLeft = DimensionType.Value(radius))
        fun top(radius: Float) = Radii.Zero.copy(topLeft = DimensionType.Value(radius), topRight = DimensionType.Value(radius))
        fun right(radius: Float) = Radii.Zero.copy(topRight = DimensionType.Value(radius), bottomRight = DimensionType.Value(radius))
        fun bottom(radius: Float) = Radii.Zero.copy(bottomLeft = DimensionType.Value(radius), bottomRight = DimensionType.Value(radius))

        fun left(@DimenRes radius: Int) = Radii.Zero.copy(topLeft = DimensionType.Value(radius), bottomLeft = DimensionType.Value(radius))
        fun top(@DimenRes radius: Int) = Radii.Zero.copy(topLeft = DimensionType.Value(radius), topRight = DimensionType.Value(radius))
        fun right(@DimenRes radius: Int) = Radii.Zero.copy(topRight = DimensionType.Value(radius), bottomRight = DimensionType.Value(radius))
        fun bottom(@DimenRes radius: Int) = Radii.Zero.copy(bottomLeft = DimensionType.Value(radius), bottomRight = DimensionType.Value(radius))
    }

    data class Single(val value: DimensionType) : RadiusType {
        constructor(value: Float) : this(DimensionType.Value(value))
        constructor(@DimenRes dimenId: Int) : this(DimensionType.Res(dimenId))
    }

    data class Radii(
        val topLeft: DimensionType,
        val topRight: DimensionType,
        val bottomRight: DimensionType,
        val bottomLeft: DimensionType,
    ) : RadiusType {
        companion object {
            val Zero = Radii(DimensionType.Zero, DimensionType.Zero, DimensionType.Zero, DimensionType.Zero)

            operator fun invoke() = Zero
        }
        constructor(
            topLeft: Float,
            topRight: Float,
            bottomRight: Float,
            bottomLeft: Float,
        ) : this(DimensionType.Value(topLeft), DimensionType.Value(topRight), DimensionType.Value(bottomRight), DimensionType.Value(bottomLeft))

        constructor(
            @DimenRes topLeft: Int,
            @DimenRes topRight: Int,
            @DimenRes bottomRight: Int,
            @DimenRes bottomLeft: Int,
        ) : this(DimensionType.Res(topLeft), DimensionType.Res(topRight), DimensionType.Res(bottomRight), DimensionType.Res(bottomLeft))
    }
}

sealed interface Shape {
    data class Rect(val param: RadiusType) : Shape {
        constructor(radius: Float) : this(RadiusType(radius))
        constructor(@DimenRes radius: Int) : this(RadiusType(radius))

        companion object {
            val Square = Rect(RadiusType.Radii.Zero)

            operator fun invoke() = Square

            fun left(radius: Float) = RadiusType.left(radius)
            fun top(radius: Float) = RadiusType.top(radius)
            fun right(radius: Float) = RadiusType.right(radius)
            fun bottom(radius: Float) = RadiusType.bottom(radius)

            fun left(@DimenRes radius: Int) = RadiusType.left(radius)
            fun top(@DimenRes radius: Int) = RadiusType.top(radius)
            fun right(@DimenRes radius: Int) = RadiusType.right(radius)
            fun bottom(@DimenRes radius: Int) = RadiusType.bottom(radius)
        }
    }
    data class Circle(val radius: DimensionType) : Shape {
        constructor(radius: Float) : this(DimensionType.Value(radius))
        constructor(@DimenRes radius: Int) : this(DimensionType.Res(radius))
    }
    data object Borderless : Shape
}

sealed interface DrawableType {
    companion object {
        operator fun invoke(
            @DrawableRes drawableId: Int,
            state: StateType = StateType.Undefined,
        ) = Res(drawableId, state)

        operator fun invoke(
            drawable: Drawable,
            state: StateType = StateType.Undefined,
        ) = Value(drawable, state)

        operator fun invoke(
            color: ColorType,
            shape: Shape = Shape.Rect(),
            style: ShapeStyle = ShapeStyle.Fill,
            state: StateType = StateType.Undefined,
        ) = Colored(color, shape, style, state)

        fun color(
            @ColorInt color: Int,
            shape: Shape = Shape.Rect(),
            style: ShapeStyle = ShapeStyle.Fill,
            state: StateType = StateType.Undefined,
        ) = Colored(ColorType.Value(color), shape, style, state)

        fun colorRes(
            @ColorRes color: Int,
            shape: Shape = Shape.Rect(),
            style: ShapeStyle = ShapeStyle.Fill,
            state: StateType = StateType.Undefined,
        ) = Colored(ColorType.Res(color), shape, style, state)

        fun colorAttr(
            @AttrRes colorAttr: Int,
            shape: Shape = Shape.Rect(),
            style: ShapeStyle = ShapeStyle.Fill,
            state: StateType = StateType.Undefined,
        ) = Colored(ColorType.Attr(colorAttr), shape, style, state)
    }
    data class Res(
        @DrawableRes val drawableId: Int,
        val state: StateType = StateType.Undefined,
    ) : DrawableType
    data class Value(
        val drawable: Drawable,
        val state: StateType = StateType.Undefined,
    ) : DrawableType
    data class Colored(
        val color: ColorType,
        val shape: Shape = Shape.Rect(),
        val style: ShapeStyle = ShapeStyle.Fill,
        val state: StateType = StateType.Undefined,
    ) : DrawableType
}

data class RippleType(
    val color: ColorType = ColorType.Res(R.color.abc_color_highlight_material),
    val shape: Shape = Shape.Rect(),
    val state: StateType = StateType.Undefined,
) {
    companion object {
        fun color(
            @ColorInt color: Int,
            shape: Shape = Shape.Rect(),
            state: StateType = StateType.Undefined,
        ) = RippleType(ColorType.Value(color), shape, state)

        fun colorRes(
            @ColorRes colorId: Int,
            shape: Shape = Shape.Rect(),
            state: StateType = StateType.Undefined,
        ) = RippleType(ColorType.Res(colorId), shape, state)

        fun colorAttr(
            @AttrRes colorAttr: Int,
            shape: Shape = Shape.Rect(),
            state: StateType = StateType.Undefined,
        ) = RippleType(ColorType.Attr(colorAttr), shape, state)
    }
}

fun View.background(ripple: RippleType? = null, vararg layers: DrawableType) {
    background = context.drawable(ripple, *layers)
}

fun View.foreground(ripple: RippleType? = null, vararg layers: DrawableType) {
    foreground = context.drawable(ripple, *layers)
}

fun Context.drawable(
    ripple: RippleType? = null,
    vararg layers: DrawableType,
): Drawable {
    TODO("Not yet implemented")
}




fun <V : View> V.rippleForeground(cornerRadius: Float): V = ripple(cornerRadius, toForeground = true)

fun <V : View> V.rippleBackground(cornerRadius: Float, backgroundColor: Int = Color.TRANSPARENT): V = ripple(cornerRadius, backgroundColor, toForeground = false)

private fun <V : View> V.ripple(cornerRadius: Float, contentColor: Int = Color.TRANSPARENT, toForeground: Boolean): V {
    val content = if (contentColor != 0) RoundCornersDrawable.fill(contentColor, cornerRadius) else null
    return ripple(cornerRadius, content, toForeground)
}

private fun <V : View> V.ripple(cornerRadius: Float, content: Drawable?, toForeground: Boolean): V {
    val rippleColor = R.color.abc_color_highlight_material
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

@SuppressLint("NewApi") // the check is in legacyMode
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

    @SuppressLint("NewApi") // the check is in legacyMode
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
