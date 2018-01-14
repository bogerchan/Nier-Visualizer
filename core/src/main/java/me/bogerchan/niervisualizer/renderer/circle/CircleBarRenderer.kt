package me.bogerchan.niervisualizer.renderer.circle

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.animation.LinearInterpolator
import me.bogerchan.niervisualizer.renderer.IRenderer
import me.bogerchan.niervisualizer.util.NierAnimator


/**
 * Thanks to the project of android-visualizer by Felix Palmer
 * I added more cool visual effects on this basis.
 *
 * Created by BogerChan on 2017/12/9.
 */
class CircleBarRenderer(
        private val paint: Paint = getDefaultPaint(),
        private val divisions: Int = 4,
        private val type: Type = Type.TYPE_A,
        private val modulationStrength: Float = 0.4f,
        private val amplification: Float = 1f,
        private val animator: NierAnimator = getDefaultAnimator()) : IRenderer {

    enum class Type {
        TYPE_A, TYPE_B, TYPE_A_AND_TYPE_B
    }

    private var mAggresive = 0.4f
    private var mModulation = 0.0
    private var mAngleModulation = 0f
    private lateinit var mFFTPoints: FloatArray
    private val mLastDrawArea = Rect()

    companion object {
        private fun getDefaultPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 8f
            color = Color.parseColor("#e6ebfe")
        }

        private fun getDefaultAnimator() = NierAnimator(interpolator = LinearInterpolator(),
                duration = 20000,
                values = floatArrayOf(0f, 360f))
    }

    override fun onStart(captureSize: Int) {
        mFFTPoints = FloatArray(captureSize * 4)
        animator.start()
    }

    override fun onStop() {
        animator.stop()
    }

    override fun calculate(drawArea: Rect, data: ByteArray) {
        if (mLastDrawArea != drawArea) {
            mLastDrawArea.set(drawArea)
        }
        val drawHeight = drawArea.height()

        for (i in 0 until data.size / divisions) {
            // Calculate dbValue
            val rfk = data[divisions * i]
            val ifk = data[divisions * i + 1]
            val magnitude = (rfk * rfk + ifk * ifk).toFloat()
            val dbValue = (75 * Math.log10(magnitude.toDouble()).toFloat() * amplification)
                    .let { if (it < 20f) 20f else it }
            val cartPoint = when (type) {
                Type.TYPE_A -> floatArrayOf((i * divisions).toFloat() / (data.size - 1), drawHeight / 2f)
                Type.TYPE_B -> floatArrayOf((i * divisions).toFloat() / (data.size - 1), drawHeight / 2f - dbValue)
                Type.TYPE_A_AND_TYPE_B -> floatArrayOf((i * divisions).toFloat() / (data.size - 1), drawHeight / 2f - dbValue)
            }

            val polarPoint = toPolar(cartPoint, drawArea)
            mFFTPoints[i * 4] = polarPoint[0]
            mFFTPoints[i * 4 + 1] = polarPoint[1]

            val cartPoint2 = when (type) {
                Type.TYPE_A -> floatArrayOf((i * divisions).toFloat() / (data.size - 1), drawHeight / 2f + dbValue)
                Type.TYPE_B -> floatArrayOf((i * divisions).toFloat() / (data.size - 1), drawHeight / 2f)
                Type.TYPE_A_AND_TYPE_B -> floatArrayOf((i * divisions).toFloat() / (data.size - 1), drawHeight / 2f + dbValue)
            }

            val polarPoint2 = toPolar(cartPoint2, drawArea)
            mFFTPoints[i * 4 + 2] = polarPoint2[0]
            mFFTPoints[i * 4 + 3] = polarPoint2[1]
        }
    }

    override fun render(canvas: Canvas) {
        canvas.save()
        canvas.rotate(animator.computeCurrentValue(),
                (mLastDrawArea.left + mLastDrawArea.right) / 2F,
                (mLastDrawArea.top + mLastDrawArea.bottom) / 2F)
        canvas.drawLines(mFFTPoints, paint)
        canvas.restore()
    }

    override fun getInputDataType() = IRenderer.DataType.FFT

    private fun toPolar(cartesian: FloatArray, rect: Rect): FloatArray {
        val cX = (rect.width() / 2).toDouble()
        val cY = (rect.height() / 2).toDouble()
        val angle = cartesian[0].toDouble() * 2.0 * Math.PI
        val radius = (rect.width() / 2 * (1 - mAggresive) + mAggresive * cartesian[1] / 2) * (1 - modulationStrength + modulationStrength * (1 + Math.sin(mModulation)) / 2)
        return floatArrayOf((cX + radius * Math.sin(angle + mAngleModulation)).toFloat(), (cY + radius * Math.cos(angle + mAngleModulation)).toFloat())
    }
}