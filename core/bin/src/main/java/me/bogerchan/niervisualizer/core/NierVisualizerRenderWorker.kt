package me.bogerchan.niervisualizer.core

import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import android.util.Log
import android.view.SurfaceView
import me.bogerchan.niervisualizer.renderer.IRenderer
import me.bogerchan.niervisualizer.util.FpsHelper
import me.bogerchan.niervisualizer.util.clear
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by BogerChan on 2017/11/26.
 */
class NierVisualizerRenderWorker {

    companion object {

        val MSG_RENDER = 0
        val MSG_START = 1
        val MSG_STOP = 2
        val MSG_UPDATE_FFT = 3
        val MSG_UPDATE_WAVE = 4

        val STATE_INIT = 0
        val STATE_START = 1
        val STATE_STOP = 2
    }

    class RenderCore(val captureSize: Int, val surfaceView: SurfaceView, val renderers: Array<IRenderer>) {
        var fftData: ByteArray = kotlin.ByteArray(captureSize)
        var waveData: ByteArray = kotlin.ByteArray(captureSize)
        var waveDataArrived = false
    }

    private val mRenderHandler by lazy {
        val ht = HandlerThread("Nier Render Thread", Process.THREAD_PRIORITY_DISPLAY)
        ht.start()
        return@lazy object : Handler(ht.looper) {
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                    MSG_RENDER -> processRenderEvent()
                    MSG_START -> processStartEvent(msg.obj as RenderCore)
                    MSG_STOP -> processStopEvent()
                    MSG_UPDATE_FFT -> processUpdateFftEvent(msg.obj as ByteArray)
                    MSG_UPDATE_WAVE -> processUpdateWaveEvent(msg.obj as ByteArray)
                }
            }
        }
    }

    private var mState = AtomicInteger(STATE_INIT)
    private val mFpsHelper by lazy { FpsHelper() }
    private val mDrawArea = Rect()
    private var mRenderCore: RenderCore? = null

    private fun processStartEvent(core: RenderCore) {
        if (mState.get() != STATE_START) {
            return
        }
        mRenderHandler.removeMessages(MSG_RENDER)
        core.renderers.forEach { it.onStart(core.captureSize) }
        mRenderCore = core
        mRenderHandler.sendEmptyMessage(MSG_RENDER)
    }

    private fun processStopEvent() {
        if (mState.get() != STATE_STOP) {
            return
        }
        val core = mRenderCore ?: return
        mRenderHandler.removeMessages(MSG_RENDER)
        core.renderers.forEach { it.onStop() }
        mRenderCore = null
    }

    private fun processUpdateFftEvent(data: ByteArray) {
        val fft = mRenderCore?.fftData ?: return
        System.arraycopy(data, 0, fft, 0, data.size)
    }

    private fun processUpdateWaveEvent(data: ByteArray) {
        val core = mRenderCore ?: return
        System.arraycopy(data, 0, core.waveData, 0, data.size)
        core.waveDataArrived = true
    }

    private fun processRenderEvent() {
        mFpsHelper.start()
        //Make sure just one
        mRenderHandler.removeMessages(MSG_RENDER)
        if (mState.get() != STATE_START) {
            return
        }
        val core = mRenderCore ?: return
        renderInternal(core)
        mFpsHelper.end()
        scheduleNextRender(mFpsHelper.nextDelayTime())
    }

    private fun scheduleNextRender(awaitTime: Long) {
        val ret = mRenderHandler.sendEmptyMessageDelayed(MSG_RENDER, awaitTime)
        if (!ret) {
            Log.e(NierConstants.TAG, "schedule next render error")
        }
    }

    private fun renderInternal(renderCore: RenderCore) {
        if (mState.get() != STATE_START) {
            return
        }
        val canvas = renderCore.surfaceView.holder.lockCanvas() ?: return
        try {
            mDrawArea.set(0, 0, canvas.width, canvas.height)
            renderCore.renderers.forEach {
                when (it.getInputDataType()) {
                    IRenderer.DataType.WAVE -> {
                        if (renderCore.waveDataArrived) {
                            it.calculate(mDrawArea, renderCore.waveData)
                        }
                    }
                    IRenderer.DataType.FFT -> it.calculate(mDrawArea, renderCore.fftData)
                }
            }
            canvas.clear()
            renderCore.renderers.forEach { it.render(canvas) }
        } finally {
            renderCore.surfaceView.holder.unlockCanvasAndPost(canvas)
        }
    }

    fun start(renderCore: RenderCore) {
        mState.set(STATE_START)
        mRenderHandler.removeMessages(MSG_START)
        Message.obtain(mRenderHandler, MSG_START, renderCore).sendToTarget()
    }

    fun stop() {
        mState.set(STATE_STOP)
        mRenderHandler.removeMessages(MSG_STOP)
        mRenderHandler.sendEmptyMessage(MSG_STOP)
    }

    fun updateFftData(data: ByteArray) {
        if (mState.get() != STATE_START) {
            return
        }
        Message.obtain(mRenderHandler, MSG_UPDATE_FFT, data).sendToTarget()
    }

    fun updateWaveData(data: ByteArray) {
        if (mState.get() != STATE_START) {
            return
        }
        Message.obtain(mRenderHandler, MSG_UPDATE_WAVE, data).sendToTarget()
    }
}