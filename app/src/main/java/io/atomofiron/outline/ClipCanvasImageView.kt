package io.atomofiron.outline

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class ClipCanvasImageView : AppCompatImageView {

    private val rect = RectF()
    private val path = Path()
    private var cornerRadius = 0f

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, a: Int) : super(context, attrs, a)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        rect.set(0f, 0f, (right - left).toFloat(), (bottom - top).toFloat())
        updatePath()
    }

    override fun draw(canvas: Canvas) {
        if (cornerRadius > 0f) canvas.clipPath(path)
        super.draw(canvas)
    }

    fun setCornerRadius(value: Float) {
        cornerRadius = value
        updatePath()
        invalidate()
    }

    private fun updatePath() = path.createRoundedCorners(rect, cornerRadius)
}