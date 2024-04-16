package demo.atomofiron.outline

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF

class ClipCanvasDelegate(private val invalidator: () -> Unit) {

    private val rect = RectF()
    private val path = Path()
    private var cornerRadius = 0f

    fun setCornerRadius(value: Float) {
        if (cornerRadius != value) {
            cornerRadius = value
            updatePath()
            invalidator()
        }
    }

    fun setSize(width: Int, height: Int) = setBounds(0f, 0f, width.toFloat(), height.toFloat())

    fun setBounds(left: Float, top: Float, right: Float, bottom: Float) {
        rect.set(left, top, right, bottom)
        updatePath()
    }

    fun onPreDraw(canvas: Canvas) {
        if (cornerRadius > 0f) canvas.clipPath(path)
    }

    private fun updatePath() = path.createRoundedCorners(rect, cornerRadius)
}