package me.bogerchan.niervisualizer.renderer.circle

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import me.bogerchan.niervisualizer.renderer.IRenderer


/**
 * Thanks to the project of android-visualizer by Felix Palmer
 *
 * Created by BogerChan on 2017/12/6.
 */
class CircleRenderer : IRenderer {

    private var mColorCounter = 0.0
    private var mModulation = 0.0
    private var mAggresive = 0.33
    private lateinit var mPoints: FloatArray
    private var mPaint: Paint
    private var mCycleColor: Boolean


    constructor(paint: Paint, cycleColor: Boolean) {
        mPaint = paint
        mCycleColor = cycleColor
    }

    constructor(cycleColor: Boolean) {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.strokeWidth = 10F
        mPaint.color = Color.CYAN
        mCycleColor = cycleColor
    }

    override fun onStart(captureSize: Int) {
        mPoints = FloatArray(captureSize * 4)
    }

    override fun onStop() {

    }

    override fun getInputDataType() = IRenderer.DataType.WAVE

    override fun calculate(drawArea: Rect, data: ByteArray) {
        if (mCycleColor) {
            cycleColor()
        }
        val height = drawArea.height().toFloat()
        for (i in 0..(data.size - 1)) {
            val cartPoint = floatArrayOf(i.toFloat() / (data.size - 1),
                    height / 2 + (data[i].toInt() and 0xFF - 128) * (height / 2) / 128)

            val polarPoint = toPolar(cartPoint, drawArea)
            mPoints[i * 4] = polarPoint[0]
            mPoints[i * 4 + 1] = polarPoint[1]

            val cartPoint2 = floatArrayOf((i + 1).toFloat() / (data.size - 1),
                    height / 2 + (data[i].toInt() and 0xFF - 128) * (height / 2) / 128)

            val polarPoint2 = toPolar(cartPoint2, drawArea)
            mPoints[i * 4 + 2] = polarPoint2[0]
            mPoints[i * 4 + 3] = polarPoint2[1]
        }
        // Controls the pulsing rate
        mModulation += 0.04
    }

    override fun render(canvas: Canvas) {
        canvas.drawLines(mPoints, mPaint)
    }

    private fun cycleColor() {
        val r = Math.floor(128 * (Math.sin(mColorCounter) + 1)).toInt()
        val g = Math.floor(128 * (Math.sin(mColorCounter + 2) + 1)).toInt()
        val b = Math.floor(128 * (Math.sin(mColorCounter + 4) + 1)).toInt()
        mPaint.color = Color.argb(128, r, g, b)
        mColorCounter += 0.03
    }

    private fun toPolar(cartesian: FloatArray, rect: Rect): FloatArray {
        val cX = (rect.width() / 2).toDouble()
        val cY = (rect.height() / 2).toDouble()
        val angle = cartesian[0].toDouble() * 2.0 * Math.PI
        val radius = (rect.width() / 2 * (1 - mAggresive) + mAggresive * cartesian[1] / 2) * (1.2 + Math.sin(mModulation)) / 2.2
        return floatArrayOf((cX + radius * Math.sin(angle)).toFloat(), (cY + radius * Math.cos(angle)).toFloat())
    }
}