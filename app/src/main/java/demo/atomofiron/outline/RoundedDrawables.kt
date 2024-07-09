package demo.atomofiron.outline

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build.VERSION_CODES.TIRAMISU
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlin.math.max
import androidx.appcompat.R as CompatR



private class Unreachable : Exception()
private val emptyIntArray = intArrayOf()


data class StateType(
    val enabled: Boolean? = null,
    val checked: Boolean? = null,
    val activated: Boolean? = null,
    val selected: Boolean? = null,
    val pressed: Boolean? = null,
    val focused: Boolean? = null,
) {
    companion object {
        val Undefined = StateType()
    }
    val isEmpty: Boolean = enabled == null && checked == null && activated == null && selected == null && pressed == null && focused == null
}

sealed interface ColorType {
    companion object {
        val RippleDefault = Res(CompatR.color.abc_color_highlight_material) // androidx.core.R?

        fun transparent(state: StateType = StateType.Undefined) = Value(Color.TRANSPARENT, state)
        fun mask(state: StateType = StateType.Undefined) = Value(Color.BLACK, state)
    }

    interface Stateful : ColorType {
        val state: StateType
    }

    data class Res(
        @ColorRes val colorId: Int,
        override val state: StateType = StateType.Undefined,
    ) : Stateful
    data class Attr(
        @AttrRes val colorAttr: Int,
        override val state: StateType = StateType.Undefined,
    ) : Stateful
    data class Value(
        @ColorInt val color: Int,
        override val state: StateType = StateType.Undefined,
    ) : Stateful

    data class Selector(val colors: List<Stateful>) : ColorType {
        constructor(vararg colors: Stateful) : this(colors.toList())
    }
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
    val paintStyle: Paint.Style,
    val strokeWidth: DimensionType,
) {
    data object Fill : ShapeStyle(Paint.Style.FILL, strokeWidth = DimensionType.Zero)
    data class Stroke(val width: DimensionType) : ShapeStyle(Paint.Style.STROKE, width) {
        constructor(@DimenRes width: Int) : this(DimensionType.Res(width))
        constructor(width: Float = 1f) : this(DimensionType.Value(width))
    }
}

sealed interface RadiusType {
    companion object {
        operator fun invoke(radius: Float) = Single(radius)
        operator fun invoke(@DimenRes dimenId: Int) = Single(dimenId)

        @RequiresApi(TIRAMISU)
        fun left(radius: Float) = Radii.Zero.copy(topLeft = DimensionType.Value(radius), bottomLeft = DimensionType.Value(radius))
        @RequiresApi(TIRAMISU)
        fun top(radius: Float) = Radii.Zero.copy(topLeft = DimensionType.Value(radius), topRight = DimensionType.Value(radius))
        @RequiresApi(TIRAMISU)
        fun right(radius: Float) = Radii.Zero.copy(topRight = DimensionType.Value(radius), bottomRight = DimensionType.Value(radius))
        @RequiresApi(TIRAMISU)
        fun bottom(radius: Float) = Radii.Zero.copy(bottomLeft = DimensionType.Value(radius), bottomRight = DimensionType.Value(radius))

        @RequiresApi(TIRAMISU)
        fun left(@DimenRes radius: Int) = Radii.Zero.copy(topLeft = DimensionType.Value(radius), bottomLeft = DimensionType.Value(radius))
        @RequiresApi(TIRAMISU)
        fun top(@DimenRes radius: Int) = Radii.Zero.copy(topLeft = DimensionType.Value(radius), topRight = DimensionType.Value(radius))
        @RequiresApi(TIRAMISU)
        fun right(@DimenRes radius: Int) = Radii.Zero.copy(topRight = DimensionType.Value(radius), bottomRight = DimensionType.Value(radius))
        @RequiresApi(TIRAMISU)
        fun bottom(@DimenRes radius: Int) = Radii.Zero.copy(bottomLeft = DimensionType.Value(radius), bottomRight = DimensionType.Value(radius))
    }

    data class Single(val value: DimensionType) : RadiusType {
        companion object {
            val Zero = Single(DimensionType.Zero)

            operator fun invoke() = Zero
        }
        constructor(value: Float) : this(DimensionType.Value(value))
        constructor(@DimenRes dimenId: Int) : this(DimensionType.Res(dimenId))
    }

    @RequiresApi(TIRAMISU)
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
            val Square = Rect(RadiusType.Single.Zero)

            operator fun invoke() = Square
        }
    }
    data class Circle(val radius: DimensionType) : ShapeType {
        constructor(radius: Float) : this(DimensionType.Value(radius))
        constructor(@DimenRes radius: Int) : this(DimensionType.Res(radius))
    }
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
        val provideOutline: Boolean = false,
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
        resolve(color) to RoundedDrawable(this, ColorType.transparent(), ShapeStyle.Fill, shape)
    }
    val list = when (ripple) {
        null -> LayerDrawable(arrayOf())
        else -> RippleDrawable(ripple.first, null, ripple.second)
    }
    layers.forEach {
        list.addLayer(resolve(it))
    }
    return list
}

internal fun Context.resolve(type: DimensionType): Float = when (type) {
    is DimensionType.Value -> type.value
    is DimensionType.Res -> resources.getDimension(type.dimenId)
}

internal fun Context.resolve(type: ColorType): ColorStateList =when (type) {
    is ColorType.Value -> ColorStateList.valueOf(type.color)
    is ColorType.Res -> ContextCompat.getColorStateList(this, type.colorId)!!
    is ColorType.Attr -> getColorListByAttr(type.colorAttr)
    is ColorType.Stateful -> throw Unreachable()
    is ColorType.Selector -> resolve(type)
}

private fun Context.resolve(list: ColorType.Selector): ColorStateList {
    val states = mutableListOf<IntArray>()
    val colors = mutableListOf<Int>()
    for (color in list.colors) {
        colors.add(resolve(color))
        states.add(color.state.attrs())
    }
    return ColorStateList(states.toTypedArray(), colors.toIntArray())
}

private fun StateType.attrs(): IntArray {
    if (isEmpty) return emptyIntArray
    return buildList {
        enabled?.let { add(android.R.attr.state_enabled * if (it) 1 else -1) }
        checked?.let { add(android.R.attr.state_checked * if (it) 1 else -1) }
        activated?.let { add(android.R.attr.state_activated * if (it) 1 else -1) }
        selected?.let { add(android.R.attr.state_selected * if (it) 1 else -1) }
        pressed?.let { add(android.R.attr.state_pressed * if (it) 1 else -1) }
        focused?.let { add(android.R.attr.state_focused * if (it) 1 else -1) }
    }.toIntArray()
}

private fun Context.resolve(color: ColorType.Stateful): Int = when (color) {
    is ColorType.Value -> color.color
    is ColorType.Res -> ContextCompat.getColor(this, color.colorId)
    is ColorType.Attr -> findResIdByAttr(color.colorAttr)
    else -> throw Unreachable()
}

private fun Context.resolve(type: DrawableType): Drawable = when (type) {
    is DrawableType.Value -> type.drawable
    is DrawableType.Res -> ContextCompat.getDrawable(this, type.drawableId)!!
    is DrawableType.Colored -> RoundedDrawable(this, type.color, type.style, type.shape)
}

fun Context.getColorByAttr(@AttrRes attr: Int): Int = ContextCompat.getColor(this, findResIdByAttr(attr))

fun Context.getColorListByAttr(@AttrRes attr: Int): ColorStateList = ContextCompat.getColorStateList(this, findResIdByAttr(attr))!!

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

fun <V : View> V.setRoundedBorder(
    borderColor: Int,
    cornerRadius: Float,
    borderWidth: Float = resources.displayMetrics.density,
): V {
    val drawable = RoundedDrawable(context, ColorType.Value(borderColor), ShapeStyle.Stroke(borderWidth), ShapeType.Rect(cornerRadius))
    foreground = drawable
    outlineProvider = drawable.outlineProvider
    clipToOutline = true
    return this
}

fun <V : View> V.clip(cornerRadius: Float): V {
    clipToOutline = true
    outlineProvider = CurveDelegate(cornerRadius, this::invalidateOutline)
    return this
}

fun Context.resolve(type: ShapeType): ShapeValueType = when (type) {
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

sealed class ShapeValueType(
    internal val topLeft: Float,
    internal val topRight: Float,
    internal val bottomRight: Float,
    internal val bottomLeft: Float,
) {
    val isZero = topLeft <= 0f && topRight <= 0f && bottomRight <= 0f && bottomLeft <= 0f

    data class Circle(private val radiusValue: Float) : ShapeValueType(
        max(0f, radiusValue),
        max(0f, radiusValue),
        max(0f, radiusValue),
        max(0f, radiusValue),
    ) {
        val radius = max(0f, radiusValue)
    }
    data class Rect(
        val topLeftRadius: Float,
        val topRightRadius: Float,
        val bottomRightRadius: Float,
        val bottomLeftRadius: Float,
    ) : ShapeValueType(
        topLeft = max(0f, topLeftRadius),
        topRight = max(0f, topRightRadius),
        bottomRight = max(0f, bottomRightRadius),
        bottomLeft = max(0f, bottomLeftRadius),
    ) {
        val allTheSame = topLeft == topRight && topRight == bottomRight && bottomRight == bottomLeft

        constructor(radius: Float) : this(radius, radius, radius, radius)
    }
}

fun Path.createCurvedCorners(bounds: RectF, radius: Float) = createCurvedCorners(bounds, radius, radius, radius, radius)

fun Path.createCurvedCorners(bounds: RectF, topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) {
    reset()
    addCurvedRect(bounds, topLeft, topRight, bottomRight, bottomLeft)
    close()
}

fun Path.createCircle(bounds: RectF, radius: Float) {
    reset()
    if (radius > 0f) {
        addCircle(bounds.left + bounds.width() / 2, bounds.top + bounds.height() / 2, radius, Path.Direction.CW)
    }
    close()
}

private const val RADIUS_TO_OFFSET = 1.2f
private const val ZERO = 0f
private const val FULL = 1f
private const val LONG_PART = 0.67f
private const val SHORT_PART = FULL - LONG_PART

/**  short   long    part
 *  v-----v--------v
 *        o--------o---
 *         .  ' |  |
 *  o   / visual|  |
 *  | .   radius|  | cubic offset Y
 *  |.----------o  |
 *  o--------------o visual radius * 1.2
 *  | cubic offset X
 */
fun Path.addCurvedRect(bounds: RectF, topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) {
    val width = bounds.width()
    val height = bounds.height()
    val tl = max(0f, topLeft) * RADIUS_TO_OFFSET
    val tr = max(0f, topRight) * RADIUS_TO_OFFSET
    val br = max(0f, bottomRight) * RADIUS_TO_OFFSET
    val bl = max(0f, bottomLeft) * RADIUS_TO_OFFSET
    val lSum = bl + tl
    val tSum = tl + tr
    val rSum = tr + br
    val bSum = br + bl
    // top left radius Y
    val tlry = if (lSum <= height) tl else height / lSum * tl
    // top left radius X
    val tlrx = if (tSum <= width) tl else width / tSum * tl
    val trrx = if (tSum <= width) tr else width / tSum * tr
    val trry = if (rSum <= height) tr else height / rSum * tr
    val brry = if (rSum <= height) br else height / rSum * br
    val brrx = if (bSum <= width) br else width / bSum * br
    val blrx = if (bSum <= width) bl else width / bSum * bl
    val blry = if (lSum <= height) bl else height / lSum * bl
    moveTo(bounds.left, bounds.top + tlry)
    // top left corner
    if (topLeft > 0f) rCubicTo(ZERO, tlry * -LONG_PART, tlrx * SHORT_PART, tlry * -FULL, tlrx * FULL, tlry * -FULL)
    rLineTo(width - tlrx - trrx, ZERO)
    // top right corner
    if (topRight > 0f) rCubicTo(trrx * LONG_PART, ZERO, trrx * FULL, trry * SHORT_PART, trrx * FULL, trry * FULL)
    rLineTo(ZERO, height - trry - brry)
    // bottom right corner
    if (bottomRight > 0f) rCubicTo(ZERO, brry * LONG_PART, brrx * -SHORT_PART, brry * FULL, brrx * -FULL, brry * FULL)
    rLineTo(-(width - brrx - blrx), ZERO)
    // bottom left corner
    if (bottomLeft > 0f) rCubicTo(blrx * -LONG_PART, ZERO, blrx * -FULL, blry * -SHORT_PART, blrx * -FULL, blry * -FULL)
    rLineTo(ZERO, -(height - blry - tlry))
}
