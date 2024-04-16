package demo.atomofiron.outline

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class ClipCanvasImageView : AppCompatImageView {

    private val clipper = ClipCanvasDelegate(::invalidate)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, a: Int) : super(context, attrs, a)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) clipper.setSize(right - left, bottom - top)
    }

    override fun draw(canvas: Canvas) {
        clipper.onPreDraw(canvas)
        super.draw(canvas)
    }

    fun setCornerRadius(value: Float) = clipper.setCornerRadius(value)
}