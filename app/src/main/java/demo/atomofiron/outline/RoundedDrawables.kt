package demo.atomofiron.outline

import android.annotation.SuppressLint
import android.content.Context
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
import android.graphics.drawable.LayerDrawable
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
import androidx.appcompat.R as CompatR
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
        val RippleDefault = Res(CompatR.color.abc_color_highlight_material) // androidx.core.R?

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

sealed class ShapeStyle(
    val paintStyle: Style,
    val strokeWidth: DimensionType,
) {
    data object Fill : ShapeStyle(Style.FILL, strokeWidth = DimensionType.Zero)
    data class Stroke(val width: DimensionType) : ShapeStyle(Style.STROKE, width) {
        constructor(@DimenRes width: Int) : this(DimensionType.Res(width))
        constructor(width: Float = 1f) : this(DimensionType.Value(width))
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

sealed interface ShapeType {
    data class Rect(val param: RadiusType) : ShapeType {
        constructor(radius: Float) : this(RadiusType(radius))
        constructor(@DimenRes radius: Int) : this(RadiusType(radius))

        companion object {
            val Square = Rect(RadiusType.Radii.Zero)

            operator fun invoke() = Square
        }
    }
    data class Circle(val radius: DimensionType) : ShapeType {
        constructor(radius: Float) : this(DimensionType.Value(radius))
        constructor(@DimenRes radius: Int) : this(DimensionType.Res(radius))
    }
    data object Borderless : ShapeType
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
            shape: ShapeType = ShapeType.Rect(),
            style: ShapeStyle = ShapeStyle.Fill,
            state: StateType = StateType.Undefined,
        ) = Colored(color, shape, style, state)

        fun color(
            @ColorInt color: Int,
            shape: ShapeType = ShapeType.Rect(),
            style: ShapeStyle = ShapeStyle.Fill,
            state: StateType = StateType.Undefined,
        ) = Colored(ColorType.Value(color), shape, style, state)

        fun colorRes(
            @ColorRes color: Int,
            shape: ShapeType = ShapeType.Rect(),
            style: ShapeStyle = ShapeStyle.Fill,
            state: StateType = StateType.Undefined,
        ) = Colored(ColorType.Res(color), shape, style, state)

        fun colorAttr(
            @AttrRes colorAttr: Int,
            shape: ShapeType = ShapeType.Rect(),
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
        val shape: ShapeType = ShapeType.Rect(),
        val style: ShapeStyle = ShapeStyle.Fill,
        val state: StateType = StateType.Undefined,
    ) : DrawableType
}

fun View.setBackground(
    rippleColor: ColorType? = null,
    clippingShape: ShapeType? = null,
    clipToOutline: Boolean = clippingShape != null,
    vararg layers: DrawableType = emptyArray(),
) {
    background = context.drawable(rippleColor, clippingShape, layers = layers)
    if (clipToOutline) {
        clipByBackground()
    }
}

fun View.setForeground(
    rippleColor: ColorType? = null,
    clippingShape: ShapeType? = null,
    clipToOutline: Boolean = clippingShape != null,
    vararg layers: DrawableType = emptyArray(),
) {
    foreground = context.drawable(rippleColor, clippingShape, layers = layers)
    if (clipToOutline) {
        clipByForeground()
    }
}

fun View.clipByBackground() {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            background?.getOutline(outline)
        }
    }
    clipToOutline = true
}

fun View.clipByForeground() {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            foreground?.getOutline(outline)
        }
    }
    clipToOutline = true
}

fun Context.drawable(
    rippleColor: ColorType? = null,
    rippleShape: ShapeType? = null,
    vararg layers: DrawableType = emptyArray(),
): Drawable {
    val rippleType = when {
        rippleColor != null -> rippleColor to ShapeType.Rect.Square
        rippleShape != null -> ColorType.RippleDefault to rippleShape
        else -> null
    }
    val ripple = rippleType?.let { (color, shape) ->
        resolve(color) to RoundCornersDrawable(this, Color.TRANSPARENT, ShapeStyle.Fill, shape)
    }
    val list = when (ripple) {
        null -> LayerDrawable(arrayOf())
        else -> RippleDrawable(ColorStateList.valueOf(ripple.first), null, ripple.second)
    }
    layers.forEach {
        list.addLayer(resolve(it))
    }
    return list
}

private fun Context.resolve(type: DimensionType): Float = when (type) {
    is DimensionType.Value -> type.value
    is DimensionType.Res -> resources.getDimension(type.dimenId)
}

private fun Context.resolve(type: ColorType): Int = when (type) {
    is ColorType.Value -> type.color
    is ColorType.Res -> ContextCompat.getColor(this, type.colorId)
    is ColorType.Attr -> getColorByAttr(type.colorAttr)
}

private fun Context.resolve(type: DrawableType): Drawable = when (type) {
    is DrawableType.Value -> type.drawable
    is DrawableType.Res -> ContextCompat.getDrawable(this, type.drawableId)!!
    is DrawableType.Colored -> RoundCornersDrawable(this, resolve(type.color), type.style, type.shape)
}

fun Context.getColorByAttr(@AttrRes attr: Int): Int = ContextCompat.getColor(this, findResIdByAttr(attr))

fun Context.findResIdByAttr(@AttrRes attr: Int): Int = findResIdsByAttr(attr)[0]

fun Context.findResIdsByAttr(@AttrRes vararg attrs: Int): IntArray {
    @SuppressLint("ResourceType")
    val array = obtainStyledAttributes(attrs)

    val values = IntArray(attrs.size)
    for (i in attrs.indices) {
        values[i] = array.getResourceId(i, 0)
    }
    array.recycle()

    return values
}


fun <V : View> V.rippleForeground(cornerRadius: Float): V = ripple(cornerRadius, toForeground = true)

fun <V : View> V.rippleBackground(cornerRadius: Float, backgroundColor: Int = Color.TRANSPARENT): V = ripple(cornerRadius, backgroundColor, toForeground = false)

private fun <V : View> V.ripple(cornerRadius: Float, contentColor: Int = Color.TRANSPARENT, toForeground: Boolean): V {
    val content = if (contentColor != 0) RoundCornersDrawable(context, contentColor, ShapeStyle.Fill, ShapeType.Rect(cornerRadius)) else null
    return ripple(cornerRadius, content, toForeground)
}

private fun <V : View> V.ripple(cornerRadius: Float, content: Drawable?, toForeground: Boolean): V {
    val rippleColor = ColorType.RippleDefault.colorId
            .let { ContextCompat.getColor(context, it) }
            .let { ColorStateList.valueOf(it) }
    val mask = RoundCornersDrawable(context, Color.BLACK, ShapeStyle.Fill, ShapeType.Rect(cornerRadius))
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
    val drawable = RoundCornersDrawable(context, borderColor, ShapeStyle.Stroke(borderWidth), ShapeType.Rect(cornerRadius))
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

fun Context.resolve(type: ShapeType): ShapeValueType = when (type) {
    is ShapeType.Borderless -> ShapeValueType.Borderless
    is ShapeType.Circle -> ShapeValueType.Circle(resolve(type.radius))
    is ShapeType.Rect -> when (type.param) {
        is RadiusType.Single -> ShapeValueType.Rect(resolve(type.param.value))
        is RadiusType.Radii -> ShapeValueType.Rect(
            resolve(type.param.topLeft),
            resolve(type.param.topRight),
            resolve(type.param.bottomRight),
            resolve(type.param.bottomLeft),
        )
    }
}

class RoundCornersDrawable private constructor(
    private val type: ShapeValueType,
    private val paint: Paint,
) : Drawable() {
    companion object {
        operator fun invoke(context: Context, color: Int, style: ShapeStyle, type: ShapeType): RoundCornersDrawable {
            val paint = Paint()
            paint.isAntiAlias = true
            paint.color = color
            paint.style = style.paintStyle
            // clip the line in the middle during drawing
            paint.strokeWidth = context.resolve(style.strokeWidth) * 2
            return RoundCornersDrawable(context.resolve(type), paint)
        }
    }

    private val path = Path()
    private val rect = RectF()

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

sealed interface ShapeValueType {
    data object Borderless : ShapeValueType
    data class Circle(val radius: Float) : ShapeValueType
    data class Rect(
        val topLeft: Float,
        val topRight: Float,
        val bottomRight: Float,
        val bottomLeft: Float,
    ) : ShapeValueType {
        constructor(radius: Float) : this(radius, radius, radius, radius)
    }
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
