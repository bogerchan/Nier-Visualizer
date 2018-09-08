package me.bogerchan.niervisualizer.renderer.columnar

import android.graphics.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import me.bogerchan.niervisualizer.renderer.IRenderer
import me.bogerchan.niervisualizer.util.NierAnimator

/**
 * Created by BogerChan on 2017/11/26.
 */
class ColumnarType2Renderer : IRenderer {

    private val mPaint: Paint
    private val mLastDrawArea = Rect()
    private lateinit var mRenderColumns: Array<RectF>
    // per column' width equals to twice of gap
    private val mGapRatio = 0.7F
    private val mRadius = 10F
    private var mHalfHeight = 0F
    private lateinit var mScaleAnimator: NierAnimator
    private lateinit var mRotateAnimator: NierAnimator
    private var mScale = 1F
    private var mRotation = 0F

    constructor(paint: Paint) {
        mPaint = paint
    }

    constructor() {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.color = Color.CYAN
    }

    override fun onStart(captureSize: Int) {
        mRenderColumns = Array(Math.min(48, captureSize)) { _ -> RectF(0F, -5F, 0F, 5F) }
        mScaleAnimator = NierAnimator().apply {
            values = floatArrayOf(1F, 1.2F, 1F)
            interpolator = BounceInterpolator()
        }
        mRotateAnimator = NierAnimator().apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 5000
            values =
                    floatArrayOf(0F, 5F, 3F, 0F, 3F, 5F, 3F, 0F, -2F, 0F, -3F, -5F, -2F, 0F)
        }
        mLastDrawArea.set(0, 0, 0, 0)
    }

    override fun onStop() {

    }

    override fun getInputDataType() = IRenderer.DataType.WAVE

    override fun calculate(drawArea: Rect, data: ByteArray) {
        if (drawArea != mLastDrawArea) {
            calculateRenderData(drawArea)
            mLastDrawArea.set(drawArea)
            mScaleAnimator.start()
            mRotateAnimator.start()
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
        mScale = mScaleAnimator.computeCurrentValue()
        mRotation = mRotateAnimator.computeCurrentValue()
    }

    private fun calculateRenderData(drawArea: Rect) {
        mHalfHeight = drawArea.height().toFloat() / 2
        val perGap = drawArea.width().toFloat() / (mRenderColumns.size * (mGapRatio + 1) + 1)
        mRenderColumns.forEachIndexed { index, rect ->
            rect.left = ((index + 1) * (1 + mGapRatio) - mGapRatio) * perGap
            rect.right = rect.left + mGapRatio * perGap
        }
    }

    override fun render(canvas: Canvas) {
        canvas.save()
        canvas.rotate(mRotation, (mLastDrawArea.left + mLastDrawArea.right) / 2F, (mLastDrawArea.top + mLastDrawArea.bottom) / 2F)
        canvas.translate(mLastDrawArea.left.toFloat(), (mLastDrawArea.top + mLastDrawArea.bottom) / 2F)
        canvas.scale(mScale, mScale)
        mRenderColumns.forEach {
            canvas.drawRoundRect(it, mRadius, mRadius, mPaint)
        }
        canvas.restore()
    }
}