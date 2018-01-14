package me.bogerchan.niervisualizer.renderer.other

import android.graphics.*
import android.view.animation.LinearInterpolator
import me.bogerchan.niervisualizer.renderer.IRenderer
import me.bogerchan.niervisualizer.util.NierAnimator

/**
 * Created by BogerChan on 2018/1/11.
 */
class ArcStaticRenderer(private val paint: Paint = getDefaultPaint(),
                        private val amplificationOuter: Float = 1F,
                        private val amplificationInner: Float = 1F,
                        private val startAngle: Float = -135F,
                        private val sweepAngle: Float = 270F,
                        private val animator: NierAnimator = getDefaultAnimator()) : IRenderer {

    companion object {
        private fun getDefaultPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 4F
            color = Color.CYAN
        }

        private fun getDefaultAnimator() = NierAnimator(interpolator = LinearInterpolator(),
                duration = 20000,
                values = floatArrayOf(0f, 360f))
    }

    private var mCenterX = 0F
    private var mCenterY = 0F
    private val mDrawArcOuterArea = RectF()
    private var mDrawArcInnerRadius = 0F
    private val mLastDrawArea = Rect()
    private val mClearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onStart(captureSize: Int) {
        animator.start()
    }

    override fun onStop() {
        animator.stop()
    }

    override fun calculate(drawArea: Rect, data: ByteArray) {
        if (mLastDrawArea == drawArea) {
            return
        }
        mLastDrawArea.set(drawArea)
        mCenterX = (drawArea.left + drawArea.right) / 2F
        mCenterY = (drawArea.top + drawArea.bottom) / 2F
        mDrawArcInnerRadius = Math.min(drawArea.width(), drawArea.height()) * 0.27F * amplificationInner
        val outerSize = Math.min(drawArea.width(), drawArea.height()) * 0.77F * amplificationOuter
        mDrawArcOuterArea.set(0F, 0F, outerSize, outerSize)
        mDrawArcOuterArea.offsetTo((drawArea.left + drawArea.right - outerSize) / 2,
                (drawArea.top + drawArea.bottom - outerSize) / 2)

    }

    override fun render(canvas: Canvas) {
        canvas.save()
        canvas.rotate(animator.computeCurrentValue(),
                mCenterX, mCenterY)
        canvas.drawArc(mDrawArcOuterArea, startAngle, sweepAngle, true, paint)
        canvas.drawCircle(mCenterX, mCenterY, mDrawArcInnerRadius, mClearPaint)
        canvas.restore()
    }

    override fun getInputDataType() = IRenderer.DataType.FFT
}