package me.bogerchan.niervisualizer

import android.media.audiofx.Visualizer
import android.util.Log
import android.view.SurfaceView
import me.bogerchan.niervisualizer.core.NierConstants
import me.bogerchan.niervisualizer.core.NierVisualizerRenderWorker
import me.bogerchan.niervisualizer.core.PeriodWorker
import me.bogerchan.niervisualizer.renderer.IRenderer
import java.lang.ref.WeakReference

/**
 * A core class to Nier visualizer which is responsible for the initialization.
 *
 * Created by BogerChan on 2017/11/26.
 */
class NierVisualizerManager {

    companion object {
        private const val DATA_SOURCE_TYPE_UNKNOWN = -1
        private const val DATA_SOURCE_TYPE_VISUALIZER = 0
        private const val DATA_SOURCE_TYPE_OUTSIDE = 1
    }

    private var mVisualizer: Visualizer? = null
    private val mRenderer by lazy { NierVisualizerRenderWorker() }
    private var mWaveBuffer: ByteArray? = null
    private var mFftBuffer: ByteArray? = null
    private val mStateBlock = Object()

    private var renderViewWR: WeakReference<SurfaceView>? = null
    private var renderers: Array<IRenderer>? = null
    private var mPeriodWorker: PeriodWorker? = null
    private var mDataSourceType: Int = DATA_SOURCE_TYPE_UNKNOWN
    private var mDataCaptureSize: Int = 0


    /**
     * Initialize Nier visualizer, you should use it in [android.app.Activity.onCreate].
     * @param audioSession system wide unique audio session identifier. see [android.media.audiofx.Visualizer].
     */
    fun init(audioSession: Int) {
        synchronized(mStateBlock) {
            mVisualizer = Visualizer(audioSession).apply {
                enabled = false
                captureSize = 512
                try {
                    scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                } catch (e: NoSuchMethodError) {
                    Log.e(NierConstants.TAG, "Can't set scaling mode", e)
                }
                measurementMode = Visualizer.MEASUREMENT_MODE_NONE
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
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

                }, Visualizer.getMaxCaptureRate(), true, true)
            }.apply {
                mDataCaptureSize = captureSize.apply {
                    mWaveBuffer = ByteArray(this)
                    mFftBuffer = ByteArray(this)
                }
            }
            mDataSourceType = DATA_SOURCE_TYPE_VISUALIZER
        }
    }

    fun init(dataSource: NVDataSource) {
        synchronized(mStateBlock) {
            mDataCaptureSize = dataSource.getDataLength().apply {
                mWaveBuffer = ByteArray(this)
                mFftBuffer = ByteArray(this)
            }
            mPeriodWorker = PeriodWorker(dataSource.getDataSamplingInterval()) {
                dataSource.apply {
                    fetchFftData()?.apply {
                        val fftBuffer = mFftBuffer ?: return@apply
                        if (size != fftBuffer.size) {
                            throw IllegalStateException("NVDataSource must provide correct fft data size as it's getDataLength() says.")
                        }
                        System.arraycopy(this, 0, fftBuffer, 0, size)
                        mRenderer.updateFftData(fftBuffer)
                    }
                    fetchWaveData()?.apply {
                        val waveBuffer = mWaveBuffer ?: return@apply
                        if (size != waveBuffer.size) {
                            throw IllegalStateException("NVDataSource must provide correct wave data size as it's getDataLength() says.")
                        }
                        System.arraycopy(this, 0, waveBuffer, 0, size)
                        mRenderer.updateFftData(waveBuffer)
                    }
                }
            }
            mDataSourceType = DATA_SOURCE_TYPE_OUTSIDE
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
            mWaveBuffer = null
            mFftBuffer = null
            when (mDataSourceType) {
                DATA_SOURCE_TYPE_VISUALIZER -> {
                    mVisualizer?.enabled = false
                    mVisualizer?.setDataCaptureListener(null, Visualizer.getMaxCaptureRate(), true, true)
                    mVisualizer?.release()
                    mVisualizer = null
                }
                DATA_SOURCE_TYPE_OUTSIDE -> {
                    mPeriodWorker?.stop()
                    mPeriodWorker = null
                }
            }
        }
    }

    /**
     * Start the render work, call it after Nier visualizer has been initialized!
     * @param view the view which Nier visualizer will render to.
     * @param newRenderers a list of renderer that control the view render work.
     */
    fun start(view: SurfaceView, newRenderers: Array<IRenderer>) {
        synchronized(mStateBlock) {
            if (newRenderers.isEmpty()) {
                throw IllegalStateException("Renders is empty!")
            }
            when (mDataSourceType) {
                DATA_SOURCE_TYPE_VISUALIZER -> {
                    val visualizer = mVisualizer
                            ?: throw IllegalStateException("You must call NierVisualizerManager.init() first!")
                    visualizer.enabled = true
                }
                DATA_SOURCE_TYPE_OUTSIDE -> {
                    val periodWorker = mPeriodWorker
                            ?: throw IllegalStateException("You must call NierVisualizerManager.init() first!")
                    periodWorker.start()
                }
                else -> {
                    throw IllegalStateException("You must call NierVisualizerManager.init() first!")
                }
            }
            renderViewWR = WeakReference(view)
            renderers = newRenderers
            mRenderer.start(NierVisualizerRenderWorker.RenderCore(mDataCaptureSize, view, newRenderers))
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
            when (mDataSourceType) {
                DATA_SOURCE_TYPE_VISUALIZER -> {
                    mVisualizer?.enabled = false
                }
                DATA_SOURCE_TYPE_OUTSIDE -> {
                    mPeriodWorker?.stop()
                }
                else -> {
                }
            }
        }
    }

    /**
     * Pause the render work.
     */
    fun pause() {
        synchronized(mStateBlock) {
            mRenderer.pause()
            when (mDataSourceType) {
                DATA_SOURCE_TYPE_VISUALIZER -> {
                    mVisualizer?.enabled = false
                }
                DATA_SOURCE_TYPE_OUTSIDE -> {
                    mPeriodWorker?.pause()
                }
                else -> {
                }
            }
        }
    }

    /**
     * Resume the render work.
     */
    fun resume() {
        synchronized(mStateBlock) {
            mRenderer.resume()
            when (mDataSourceType) {
                DATA_SOURCE_TYPE_VISUALIZER -> {
                    mVisualizer?.enabled = true
                }
                DATA_SOURCE_TYPE_OUTSIDE -> {
                    mPeriodWorker?.resume()
                }
                else -> {
                }
            }
        }
    }

    /**
     * An interface used for providing fft data or wave data, these data will be transformed to visual effect.
     */
    interface NVDataSource {

        /**
         * Tell the manager about the data sampling interval.
         * @return the data sampling interval which is millisecond of unit.
         */
        fun getDataSamplingInterval(): Long

        /**
         * Tell the manager about the data length of fft data or wave data.
         * @return the data length of fft data or wave data.
         */
        fun getDataLength(): Int

        /**
         * The manager will fetch fft data by it.
         * @return the fft data, null will be ignored by the manager.
         */
        fun fetchFftData(): ByteArray?

        /**
         * The manager will fetch wave data by it.
         * @return the wave data, null will be ignored by the manager.
         */
        fun fetchWaveData(): ByteArray?
    }
}