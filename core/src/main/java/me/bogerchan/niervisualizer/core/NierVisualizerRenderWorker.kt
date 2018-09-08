package me.bogerchan.niervisualizer.core

import android.graphics.Canvas
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.Process
import android.util.Log
import android.view.SurfaceHolder
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
        val TAG = NierConstants.tagFor("NierVisualizerRenderWorker")
        val MSG_RENDER = 0
        val MSG_START = 1
        val MSG_STOP = 2
        val MSG_PAUSE = 3
        val MSG_RESUME = 4
        val MSG_UPDATE_FFT = 5
        val MSG_UPDATE_WAVE = 6

        val STATE_INIT = 0
        val STATE_START = 1
        val STATE_STOP = 2
        val STATE_PAUSE = 3
        val STATE_QUIT = 4
    }

    class RenderCore(val captureSize: Int, val surfaceView: SurfaceView, val renderers: Array<IRenderer>) {
        var fftData: ByteArray = kotlin.ByteArray(captureSize)
        var waveData: ByteArray = kotlin.ByteArray(captureSize)
        var waveDataArrived = false
    }

    private val mRenderHandler by lazy {
        val ht = HandlerThread("Nier Render Thread", Process.THREAD_PRIORITY_URGENT_DISPLAY)
        ht.start()
        return@lazy object : Handler(ht.looper) {
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                    MSG_RENDER -> processRenderEvent()
                    MSG_START -> processStartEvent(msg.obj as RenderCore)
                    MSG_STOP -> processStopEvent()
                    MSG_PAUSE -> processPauseEvent()
                    MSG_RESUME -> processResumeEvent()
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
        mRenderHandler.apply {
            removeMessages(MSG_RENDER)
            mRenderCore = core.apply {
                renderers.forEach { it.onStart(core.captureSize) }
            }
            sendEmptyMessage(MSG_RENDER)
        }
    }

    private fun processStopEvent() {
        if (mState.get() != STATE_STOP) {
            return
        }
        mRenderCore?.apply {
            mRenderHandler.removeMessages(MSG_RENDER)
            renderers.forEach { it.onStop() }
            mRenderCore = null
        }
    }

    private fun processPauseEvent() {
        mRenderCore?.apply {
            mRenderHandler.removeMessages(MSG_RENDER)
            renderers.forEach { it.onStop() }
        }
    }

    private fun processResumeEvent() {
        mRenderCore?.apply {
            mRenderHandler.apply {
                removeMessages(MSG_RENDER)
                renderers.forEach { it.onStart(captureSize) }
                sendEmptyMessage(MSG_RENDER)
            }
        }
    }

    private fun processUpdateFftEvent(data: ByteArray) {
        mRenderCore?.fftData?.apply {
            System.arraycopy(data, 0, this, 0, data.size)

        }
    }

    private fun processUpdateWaveEvent(data: ByteArray) {
        mRenderCore?.apply {
            System.arraycopy(data, 0, waveData, 0, data.size)
            waveDataArrived = true
        }
    }

    private fun processRenderEvent() {
        if (mState.get() != STATE_START) {
            return
        }
        mRenderCore?.apply {
            mFpsHelper.start()
            //Make sure just one
            mRenderHandler.removeMessages(MSG_RENDER)
            renderInternal(this)
            mFpsHelper.end()
            scheduleNextRender(mFpsHelper.nextDelayTime())
        }
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
        renderCore.surfaceView.holder.apply {
            lockCanvas()?.apply {
                try {
                    mDrawArea.set(0, 0, width, height)
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
                    clear()
                    renderCore.renderers.forEach { it.render(this) }
                } finally {
                    unlockCanvasAndPostSafely(this)
                }
            }
        }
    }

    private fun SurfaceHolder.unlockCanvasAndPostSafely(canvas: Canvas) {
        try {
            if (mState.get() != STATE_START) {
                return
            }
            unlockCanvasAndPost(canvas)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "unlockCanvasAndPostSafely, illegal state", e)
        }
    }

    fun start(renderCore: RenderCore) {
        mState.set(STATE_START)
        mRenderHandler.removeMessages(MSG_START)
        Message.obtain(mRenderHandler, MSG_START, renderCore).sendToTarget()
    }

    fun stop() {
        mState.set(STATE_STOP)
        mRenderHandler.apply {
            removeMessages(MSG_STOP)
            sendEmptyMessage(MSG_STOP)
        }
    }

    fun pause() {
        mState.set(STATE_PAUSE)
        mRenderHandler.apply {
            removeMessages(MSG_PAUSE)
            sendEmptyMessage(MSG_PAUSE)
        }
    }

    fun resume() {
        mState.set(STATE_START)
        mRenderHandler.apply {
            removeMessages(MSG_RESUME)
            sendEmptyMessage(MSG_RESUME)
        }
    }

    fun quit() {
        mState.set(STATE_QUIT)
        mRenderHandler.looper.quitSafely()
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