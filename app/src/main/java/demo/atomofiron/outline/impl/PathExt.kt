package demo.atomofiron.outline.impl

import android.graphics.Path
import android.graphics.RectF
import kotlin.math.max


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
