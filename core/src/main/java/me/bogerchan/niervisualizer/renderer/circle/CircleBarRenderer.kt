package me.bogerchan.niervisualizer.renderer.circle

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import me.bogerchan.niervisualizer.renderer.IRenderer


/**
 * Thanks to the project of android-visualizer by Felix Palmer
 *
 * Created by BogerChan on 2017/12/9.
 */
class CircleBarRenderer : IRenderer {

    private val mPaint: Paint
    private var mColorCounter = 0.0
    private var mAggresive = 0.4f
    private var mModulation = 0.0
    private var mModulationStrength = 0.4f // 0-1
    private var mAngleModulation = 0f
    private lateinit var mFFTPoints: FloatArray
    private var mDivisions: Int
    private var mCycleColor: Boolean

    constructor(paint: Paint, divisions: Int, cycleColor: Boolean) {
        mPaint = paint
        mDivisions = divisions
        mCycleColor = cycleColor
    }

    constructor(divisions: Int, cycleColor: Boolean) {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.color = Color.CYAN
        mPaint.strokeWidth = 10F
        mDivisions = divisions
        mCycleColor = cycleColor
    }

    override fun onStart(captureSize: Int) {
        mFFTPoints = FloatArray(captureSize * 4)
    }

    override fun onStop() {

    }

    override fun calculate(drawArea: Rect, data: ByteArray) {
        if (mCycleColor) {
            cycleColor()
        }

        val drawHeight = drawArea.height()

        for (i in 0 until data.size / mDivisions) {
            // Calculate dbValue
            val rfk = data[mDivisions * i]
            val ifk = data[mDivisions * i + 1]
            val magnitude = (rfk * rfk + ifk * ifk).toFloat()
            val dbValue = 75 * Math.log10(magnitude.toDouble()).toFloat()

            val cartPoint = floatArrayOf((i * mDivisions).toFloat() / (data.size - 1), drawHeight / 2 - dbValue / 4)

            val polarPoint = toPolar(cartPoint, drawArea)
            mFFTPoints[i * 4] = polarPoint[0]
            mFFTPoints[i * 4 + 1] = polarPoint[1]

            val cartPoint2 = floatArrayOf((i * mDivisions).toFloat() / (data.size - 1), drawHeight / 2 + dbValue)

            val polarPoint2 = toPolar(cartPoint2, drawArea)
            mFFTPoints[i * 4 + 2] = polarPoint2[0]
            mFFTPoints[i * 4 + 3] = polarPoint2[1]
        }
    }

    override fun render(canvas: Canvas) {
        canvas.drawLines(mFFTPoints, mPaint)
    }

    override fun getInputDataType() = IRenderer.DataType.FFT

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
        val radius = (rect.width() / 2 * (1 - mAggresive) + mAggresive * cartesian[1] / 2) * (1 - mModulationStrength + mModulationStrength * (1 + Math.sin(mModulation)) / 2)
        return floatArrayOf((cX + radius * Math.sin(angle + mAngleModulation)).toFloat(), (cY + radius * Math.cos(angle + mAngleModulation)).toFloat())
    }
}