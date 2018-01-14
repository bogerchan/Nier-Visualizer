package me.bogerchan.niervisualizer.util

import android.os.SystemClock
import android.util.Log
import me.bogerchan.niervisualizer.core.NierConstants

/**
 * A helper for scheduling the frame for rendering.
 *
 * Created by BogerChan on 2017/11/26.
 */
class FpsHelper {

    companion object {
        val DEFAULT_CALIBRATION = 2
        val DEFAULT_TOLERATE_TIME = 2000L
    }

    private val mFrameGap = 1000 / 24
    private var mLastTrackTime: Long = -1L
    private var mAwaitTime: Int = 0
    private var mSkippedFrame = 0
    private var mLastSampleTime = -1L
    private var mFps = 0

    fun start() {
        val curTime = SystemClock.elapsedRealtime()
        val interval = curTime - mLastTrackTime
        val lastAwaitTime = mAwaitTime
        mAwaitTime = mFrameGap
        if (interval < DEFAULT_TOLERATE_TIME) {
            if (mLastTrackTime >= 0L) {
                mAwaitTime -= (interval.toInt() - lastAwaitTime)
            }
        } else {
            mFps = 0
            mLastSampleTime = curTime
        }
        mAwaitTime -= DEFAULT_CALIBRATION
        mLastTrackTime = curTime
        if (mLastSampleTime == -1L) {
            mLastSampleTime = curTime
        }
    }

    fun end() {
        val curTime = SystemClock.elapsedRealtime()
        mAwaitTime -= (curTime - mLastTrackTime).toInt()
        if (mAwaitTime < 0) {
            mSkippedFrame = - mAwaitTime / mFrameGap + 1
            mAwaitTime += mSkippedFrame * mFrameGap
            Log.d(NierConstants.TAG, "skipped frame: $mSkippedFrame, await: $mAwaitTime")
        } else {
            mSkippedFrame = 0
        }
        mLastTrackTime = curTime
        mFps ++
        if (curTime - mLastSampleTime > 1000L) {
            Log.d(NierConstants.TAG, "current fps: ${(1000.0 / (curTime - mLastSampleTime) * mFps).toInt()}")
            mFps = 0
            mLastSampleTime = curTime
        }
    }

    fun nextDelayTime(): Long {
        return mAwaitTime.toLong()
    }
}