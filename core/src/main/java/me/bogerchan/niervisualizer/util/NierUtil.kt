package me.bogerchan.niervisualizer.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * Created by BogerChan on 2017/12/7.
 */
private val clearPaint by lazy {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    return@lazy paint
}

fun Canvas.clear() {
    drawPaint(clearPaint)
}
