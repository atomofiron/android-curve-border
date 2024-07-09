package demo.atomofiron.outline

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.view.ViewOutlineProvider

class RoundedDrawable private constructor(
    type: ShapeValueType,
    private val colorList: ColorStateList,
    private val paint: Paint,
) : Drawable() {
    companion object {
        operator fun invoke(context: Context, color: ColorType, style: ShapeStyle, type: ShapeType): RoundedDrawable {
            val paint = Paint()
            paint.isAntiAlias = true
            val colorList = context.resolve(color)
            paint.color = colorList.defaultColor
            paint.style = style.paintStyle
            // clip the line in the middle during drawing
            paint.strokeWidth = context.resolve(style.strokeWidth) * 2
            return RoundedDrawable(context.resolve(type), colorList, paint)
        }
    }

    private val curveDelegate = CurveDelegate(type, ::invalidateSelf)
    private var colorFilter: ColorFilter? = null

    private var tintList: ColorStateList? = null
    private var tintFilter: ColorFilter? = null
    private var tintMode: PorterDuff.Mode? = if (SDK_INT < Q) PorterDuff.Mode.SRC_IN else null
    private var tintBlendMode: BlendMode? = if (SDK_INT >= Q) BlendMode.SRC_IN else null

    val outlineProvider: ViewOutlineProvider = curveDelegate

    override fun getOutline(outline: Outline) = curveDelegate.getOutline(outline)

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        curveDelegate.setBounds(left, top, right, bottom)
    }

    override fun draw(canvas: Canvas) {
        when {
            paint.color == 0 -> return
            paint.alpha < 0f -> return
            !isVisible -> return
        }
        curveDelegate.clip(canvas)
        curveDelegate.run { canvas.draw(paint) }
    }

    override fun setAlpha(alpha: Int) = paint.setAlpha(alpha)

    override fun setColorFilter(colorFilter: ColorFilter?) {
        this.colorFilter = colorFilter
    }

    override fun getColorFilter(): ColorFilter? = colorFilter

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        paint.color = colorList.getColorForState(state, colorList.defaultColor)
        updateTint()
        return true
    }

    override fun setTintList(tint: ColorStateList?) {
        tintList = tint
        updateTint()
    }

    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        this.tintMode = tintMode
        tintBlendMode = null
        updateTint()
    }

    override fun setTintBlendMode(blendMode: BlendMode?) {
        this.tintMode = null
        tintBlendMode = blendMode
        updateTint()
    }

    @SuppressLint("NewApi")
    private fun updateTint() {
        tintFilter = tintList
            ?.run { getColorForState(state, defaultColor) }
            ?.let { color ->
                tintBlendMode?.let { BlendModeColorFilter(color, it) }
                    ?: tintMode?.let { PorterDuffColorFilter(color, it) }
            }
    }
}
