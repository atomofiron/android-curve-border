package demo.atomofiron.outline

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat

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
