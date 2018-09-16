package me.bogerchan.niervisualizer.core

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Boger Chan on 2018/9/16.
 */
class PeriodWorker(private val intervalMs: Long, private val callback: () -> Unit) {

    private var mLooper: Looper? = null
    private var mHandler: Handler? = null
    private val isWorking = AtomicBoolean(false)

    fun start() {
        if (!isWorking.compareAndSet(false, true)) {
            return
        }
        mLooper = HandlerThread("Nier Visualizer ds worker").let {
            it.start()
            it.looper
        }.apply {
            mHandler = Handler(this) {
                if (!isWorking.get()) {
                    return@Handler true
                }
                mHandler?.sendEmptyMessageDelayed(0, intervalMs)
                callback()
                true
            }.apply {
                sendEmptyMessageDelayed(0, 0)
            }
        }
    }

    fun pause() {
        if (!isWorking.compareAndSet(true, false)) {
            return
        }
        mHandler?.removeMessages(0)
    }

    fun resume() {
        if (!isWorking.compareAndSet(false, true)) {
            return
        }
        mHandler?.sendEmptyMessageDelayed(0, 0)
    }

    fun stop() {
        if (isWorking.compareAndSet(true, false)) {
            mHandler?.removeMessages(0)
        }
        mHandler = null
        mLooper?.quit()
        mLooper = null
    }
}