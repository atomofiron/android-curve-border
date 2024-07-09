package demo.atomofiron.outline

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider


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
