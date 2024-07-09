package demo.atomofiron.outline

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.os.Build.VERSION_CODES.TIRAMISU as AndroidT
import android.view.ViewOutlineProvider

val isCurvedOutlineWork = SDK_INT >= AndroidT
val isCurveUnavailable = SDK_INT < Build.VERSION_CODES.R

class CurveDelegate internal constructor(
    private var shape: ShapeValueType,
    private val invalidator: () -> Unit,
) : ViewOutlineProvider() {
    companion object {
        var forceLegacy = false
        val legacyMode get() = forceLegacy || isCurveUnavailable // !isCurvedOutlineWork
    }

    private val rectf = RectF()
    private val rect = Rect()
    private val path = Path()

    constructor(invalidator: () -> Unit) : this(0f, invalidator)

    constructor(
        cornerRadius: Float,
        invalidator: () -> Unit,
    ) : this(ShapeValueType.Rect(cornerRadius), invalidator)

    constructor(
        context: Context,
        type: ShapeType,
        invalidator: () -> Unit,
    ) : this(context.resolve(type), invalidator)

    override fun getOutline(view: View, outline: Outline) = outline.apply(0, 0, view.width, view.height)

    fun getOutline(outline: Outline) = outline.apply(rect.left, rect.top, rect.right, rect.bottom)

    fun clip(canvas: Canvas) {
        if (!path.isEmpty) canvas.clipPath(path)
    }

    fun Canvas.draw(paint: Paint) = drawPath(path, paint)

    fun setCornerRadius(value: Float) = setCornerRadius(value, value, value, value)

    fun setCornerRadius(
        topLeft: Float = asRect()?.topLeft ?: 0f,
        topRight: Float = asRect()?.topRight ?: 0f,
        bottomRight: Float = asRect()?.bottomRight ?: 0f,
        bottomLeft: Float = asRect()?.bottomLeft ?: 0f,
    ) {
        val new = ShapeValueType.Rect(topLeft, topRight, bottomRight, bottomLeft)
        setShape(new)
    }

    fun setCircleRadius(value: Float) {
        val new = ShapeValueType.Circle(value)
        setShape(new)
    }

    fun setSize(width: Int, height: Int) = setBounds(0, 0, width, height)

    fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        if (rect.left == left && rect.top == top && rect.right == right && rect.bottom == bottom) {
            return
        }
        rect.set(left, top, right, bottom)
        rectf.set(rect)
        updatePath()
        invalidator()
    }

    private fun setShape(new: ShapeValueType) {
        if (new != shape) {
            shape = new
            updatePath()
            invalidator()
        }
    }

    private fun updatePath() {
        val shape = shape
        when {
            shape is ShapeValueType.Circle -> path.createCircle(rectf, shape.radius)
            shape.isZero -> path.addRect(rectf, Path.Direction.CW)
            shape !is ShapeValueType.Rect -> Unit // smartcast
            !legacyMode -> path.createCurvedCorners(rectf, shape.topLeft, shape.topRight, shape.bottomRight, shape.bottomLeft)
            shape.allTheSame -> path.addRoundRect(rectf, shape.topLeft, shape.topLeft, Path.Direction.CW)
            else -> {
                val radii = shape.run { floatArrayOf(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft) }
                path.addRoundRect(rectf, radii, Path.Direction.CW)
            }
        }
    }

    private fun Outline.apply(left: Int, top: Int, right: Int, bottom: Int) {
        val shape = shape
        when {
            shape is ShapeValueType.Circle -> {
                val radius = shape.radius.toInt()
                // or fit?
                // val radius = (min(right - left, bottom - top) / 2).coerceAtMost(shape.radius.toInt())
                val centerX = rect.centerX()
                val centerY = rect.centerY()
                setOval(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            }
            shape.isZero -> setRect(rect)
            !legacyMode -> setPath(path)
            shape !is ShapeValueType.Rect -> Unit // smartcast
            shape.allTheSame -> setRoundRect(left, top, right, bottom, shape.topLeft)
            else -> Unit // we can't clip to outline with different radii
        }
    }

    private fun asRect(): ShapeValueType.Rect? = shape as? ShapeValueType.Rect
}
