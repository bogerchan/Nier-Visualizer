package me.bogerchan.niervisualizer.util

import android.view.animation.DecelerateInterpolator

/**
 * Created by BogerChan on 2017/12/3.
 */
class KeyFrameMaker {

    private lateinit var mDestWaveData: ByteArray
    private lateinit var mDestFftData: ByteArray
    private lateinit var mPrevWaveData: ByteArray
    private lateinit var mPrevFftData: ByteArray
    private val mWaveAnimator = NierAnimator(duration = 300, values = floatArrayOf(0F, 1F), interpolator = DecelerateInterpolator(), repeatable = false)
    private val mFftAnimator = NierAnimator(duration = 300, values = floatArrayOf(0F, 1F), interpolator = DecelerateInterpolator(), repeatable = false)

    lateinit var computedWaveData: ByteArray
    lateinit var computedFftData: ByteArray

    fun prepare(captureSize: Int) {
        mWaveAnimator.start()
        mFftAnimator.start()
        mDestWaveData = ByteArray(captureSize) { -128 }
        mPrevWaveData = ByteArray(captureSize) { -128 }
        computedWaveData = ByteArray(captureSize) { -128 }
        mDestFftData = ByteArray(captureSize)
        mPrevFftData = ByteArray(captureSize)
        computedFftData = ByteArray(captureSize)
    }

    fun updateWaveData(waveData: ByteArray) {
        System.arraycopy(waveData, 0, mDestWaveData, 0, mDestWaveData.size)
        System.arraycopy(computedWaveData, 0, mPrevWaveData, 0, mPrevWaveData.size)
//        System.arraycopy(waveData, 0, computedWaveData, 0, computedWaveData.size)
//        System.arraycopy(waveData, 0, mPrevWaveData, 0, mPrevWaveData.size)
        mWaveAnimator.reset()
    }

    fun updateFftData(fftData: ByteArray) {
        System.arraycopy(fftData, 0, mDestFftData, 0, mDestFftData.size)
        System.arraycopy(computedFftData, 0, mPrevFftData, 0, mPrevFftData.size)
//        System.arraycopy(fftData, 0, computedFftData, 0, computedFftData.size)
//        System.arraycopy(fftData, 0, mPrevFftData, 0, mPrevFftData.size)
        mFftAnimator.reset()
    }

    private fun ByteArray.originMap(transform: (Int, Byte) -> Byte) {
        for ((index, byte) in this.withIndex()) {
            this[index] = transform(index, byte)
        }
    }

    fun makeKeyFrame() {
        val waveFrac = mWaveAnimator.computeCurrentValue()
        val fftFrac = mFftAnimator.computeCurrentValue()
        if (mWaveAnimator.hasValueUpdated) {
            computedWaveData.originMap { idx, _ ->
                (((mDestWaveData[idx].toInt() and 0xFF) - (mPrevWaveData[idx].toInt() and 0xFF)) * waveFrac + (mPrevWaveData[idx].toInt() and 0xFF)).toByte()
            }
        }
        if (mFftAnimator.hasValueUpdated) {
            computedFftData.originMap { idx, _ ->
                ((mDestFftData[idx] - mPrevFftData[idx]) * fftFrac + mPrevFftData[idx]).toByte()
            }
        }
    }
}