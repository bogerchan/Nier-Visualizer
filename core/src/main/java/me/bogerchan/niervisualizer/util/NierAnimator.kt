package me.bogerchan.niervisualizer.util

import android.animation.TimeInterpolator
import android.os.SystemClock
import android.view.animation.LinearInterpolator

/**
 * Created by BogerChan on 2017/12/3.
 */
class NierAnimator {

    companion object {
        val DEFAULT_INTERPOLATOR = LinearInterpolator()
        val DEFAULT_DURATION = 2000
        val DEFAULT_VALUES = floatArrayOf(1F, 1.2F, 1.1F, 1F)
    }

    var interpolator: TimeInterpolator = DEFAULT_INTERPOLATOR
    var duration = DEFAULT_DURATION
    var values = DEFAULT_VALUES
    private var mFraction = 0F
    private var mLastRecordTime = 0L
    private var isRunning = false
    private var mFractionDuration = 0F

    fun start() {
        mLastRecordTime = SystemClock.elapsedRealtime()
        isRunning = true
        mFractionDuration = duration.toFloat() / (values.size - 1)
    }

    fun stop() {
        mFraction = .0F
        isRunning = false
    }

    fun pause() {
        isRunning = false
    }

    fun computeCurrentValue(): Float {
        if (!isRunning) {
            return mFraction
        }
        val curTime = SystemClock.elapsedRealtime()
        val interval = curTime - mLastRecordTime
        val curIdx = (interval % duration / mFractionDuration).toInt()
        mFraction = interval % mFractionDuration / mFractionDuration
        mFraction = interpolator.getInterpolation(mFraction)
        mFraction = values[curIdx] + (values[curIdx + 1] - values[curIdx]) * mFraction
        return mFraction
    }
}