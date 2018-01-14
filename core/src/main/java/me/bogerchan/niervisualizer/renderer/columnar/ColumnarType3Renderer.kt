package me.bogerchan.niervisualizer.renderer.columnar

import android.graphics.*
import me.bogerchan.niervisualizer.renderer.IRenderer

/**
 * Created by BogerChan on 2017/11/26.
 */
class ColumnarType3Renderer : IRenderer {

    private val mPaint: Paint
    private val mLastDrawArea = Rect()
    private lateinit var mRenderColumns: Array<RectF>
    // per column' width equals to twice of gap
    private val mGapRatio = 0.7F
    private val mRadius = 10F
    private var mHalfHeight = 0F
    private var mColorCounter = 0.0

    constructor(paint: Paint) {
        mPaint = paint
    }

    constructor() {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.color = Color.CYAN
    }

    override fun onStart(captureSize: Int) {
        mRenderColumns = Array(Math.min(40, captureSize),
                { _ -> RectF(0F, -5F, 0F, 5F) })
        mLastDrawArea.set(0, 0, 0, 0)
    }

    override fun onStop() {

    }

    override fun getInputDataType() = IRenderer.DataType.WAVE

    override fun calculate(drawArea: Rect, data: ByteArray) {
        if (drawArea != mLastDrawArea) {
            calculateRenderData(drawArea)
            mLastDrawArea.set(drawArea)
        }
        updateWave(data)
    }

    private fun transformWaveValue(value: Byte, rectF: RectF) {
        rectF.bottom = ((value.toInt() and 0xFF).toFloat() - 128F) / 128F * mHalfHeight
        rectF.bottom = if (rectF.bottom == 0F) 5F else rectF.bottom
        rectF.top = -rectF.bottom
    }

    private fun updateWave(data: ByteArray) {
        if (mRenderColumns.size >= data.size) {
            data.forEachIndexed { index, byte ->
                transformWaveValue(byte, mRenderColumns[index])
            }
        } else {
            val step = data.size / mRenderColumns.size
            mRenderColumns.forEachIndexed { index, rectF ->
                transformWaveValue(data[index * step], rectF)
            }
        }
        cycleColor()
    }

    private fun calculateRenderData(drawArea: Rect) {
        mHalfHeight = drawArea.height() / 3F
        val perGap = drawArea.width().toFloat() / (mRenderColumns.size * (mGapRatio + 1) + 1)
        mRenderColumns.forEachIndexed { index, rect ->
            rect.left = ((index + 1) * (1 + mGapRatio) - mGapRatio) * perGap
            rect.right = rect.left + mGapRatio * perGap
        }
    }

    private fun cycleColor() {
        val r = Math.floor(128 * (Math.sin(mColorCounter) + 1)).toInt()
        val g = Math.floor(128 * (Math.sin(mColorCounter + 2) + 1)).toInt()
        val b = Math.floor(128 * (Math.sin(mColorCounter + 4) + 1)).toInt()
        mPaint.color = Color.argb(128, r, g, b)
        mColorCounter += 0.03
    }


    override fun render(canvas: Canvas) {
        canvas.save()
        canvas.translate(mLastDrawArea.left.toFloat(), (mLastDrawArea.top + mLastDrawArea.bottom) / 2F)
        mRenderColumns.forEach {
            canvas.drawRoundRect(it, mRadius, mRadius, mPaint)
        }
        canvas.restore()
    }
}