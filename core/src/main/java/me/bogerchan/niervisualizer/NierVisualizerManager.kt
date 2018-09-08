package me.bogerchan.niervisualizer

import android.media.audiofx.Visualizer
import android.view.SurfaceView
import me.bogerchan.niervisualizer.core.NierVisualizerRenderWorker
import me.bogerchan.niervisualizer.renderer.IRenderer
import java.lang.ref.WeakReference

/**
 * A core class to Nier visualizer which is responsible for the initialization.
 *
 * Created by BogerChan on 2017/11/26.
 */
class NierVisualizerManager {

    private var mVisualizer: Visualizer? = null
    private val mRenderer by lazy { NierVisualizerRenderWorker() }
    private var mWaveBuffer: ByteArray? = null
    private var mFftBuffer: ByteArray? = null
    private val mStateBlock = Object()

    private val mDataCaptureListener by lazy {
        object : Visualizer.OnDataCaptureListener {
            override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                val fftBuffer = mFftBuffer ?: return
                if (fft == null || fft.size != fftBuffer.size) {
                    return
                }
                System.arraycopy(fft, 0, fftBuffer, 0, fft.size)
                mRenderer.updateFftData(fftBuffer)
            }

            override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                val waveBuffer = mWaveBuffer ?: return
                if (waveform == null || waveform.size != waveBuffer.size) {
                    return
                }
                System.arraycopy(waveform, 0, waveBuffer, 0, waveform.size)
                mRenderer.updateWaveData(waveBuffer)
            }

        }
    }

    var renderViewWR: WeakReference<SurfaceView>? = null
    var renderers: Array<IRenderer>? = null

    /**
     * Initialize Nier visualizer, you should use it in [android.app.Activity.onCreate].
     * @param audioSession system wide unique audio session identifier. see [android.media.audiofx.Visualizer].
     */
    fun init(audioSession: Int) {
        synchronized(mStateBlock) {
            val visualizer = Visualizer(audioSession).apply {
                enabled = false
                captureSize = 512
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                measurementMode = Visualizer.MEASUREMENT_MODE_NONE
                setDataCaptureListener(mDataCaptureListener, Visualizer.getMaxCaptureRate(), true, true)
                enabled = true
            }
            mWaveBuffer = ByteArray(visualizer.captureSize)
            mFftBuffer = ByteArray(visualizer.captureSize)
            mVisualizer = visualizer
        }
    }

    /**
     * Release Nier visualizer instance, you should use it in [android.app.Activity.onDestroy].
     */
    fun release() {
        synchronized(mStateBlock) {
            renderViewWR = null
            renderers = null
            mRenderer.stop()
            mRenderer.quit()
            mVisualizer?.enabled = false
            mVisualizer?.setDataCaptureListener(null, Visualizer.getMaxCaptureRate(), true, true)
            mVisualizer?.release()
            mVisualizer = null
            mWaveBuffer = null
            mFftBuffer = null
        }
    }

    /**
     * Start the render work, call it after Nier visualizer has been initialized!
     * @param view the view which Nier visualizer will render to.
     * @param newRenderers a list of renderer that control the view render work.
     */
    fun start(view: SurfaceView, newRenderers: Array<IRenderer>) {
        synchronized(mStateBlock) {
            val visualizer = mVisualizer
                    ?: throw IllegalStateException("You must call NierVisualizerManager.init() first!")
            if (newRenderers.isEmpty()) {
                throw IllegalStateException("Renders is empty!")
            }
            renderViewWR = WeakReference(view)
            renderers = newRenderers
            mRenderer.start(NierVisualizerRenderWorker.RenderCore(visualizer.captureSize, view, newRenderers))
        }
    }

    /**
     * Stop the render work.
     */
    fun stop() {
        synchronized(mStateBlock) {
            renderViewWR = null
            renderers = null
            mRenderer.stop()
        }
    }

    /**
     * Pause the render work.
     */
    fun pause() {
        synchronized(mStateBlock) {
            mRenderer.pause()
        }
    }

    /**
     * Resume the render work.
     */
    fun resume() {
        synchronized(mStateBlock) {
            mRenderer.resume()
        }
    }
}