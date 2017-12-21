package me.bogerchan.niervisualizer.renderer

import android.graphics.Canvas
import android.graphics.Rect

/**
 * Nier visualizer renderer' interface, that control a render behavior to a View.
 *
 * Created by BogerChan on 2017/11/26.
 */
interface IRenderer {

    /**
     * Visualizer data type.
     */
    enum class DataType {
        FFT, WAVE
    }

    /**
     * The renderer can do some initialization here.
     * @param captureSize current capture size.
     */
    fun onStart(captureSize: Int)

    /**
     * The renderer can do some release here.
     */
    fun onStop()

    /**
     * Calculate render data, do some pure calculating work here.
     * @param drawArea current canvas draw area.
     * @param data current capture data which it's type is one of [DataType].
     */
    fun calculate(drawArea: Rect, data: ByteArray)

    /**
     * Render calculated data.
     * @param canvas the canvas on which renderer will drawn.
     */
    fun render(canvas: Canvas)

    /**
     * Return The type of data used by the renderer.
     *
     * @return the type of data.
     */
    fun getInputDataType(): DataType
}