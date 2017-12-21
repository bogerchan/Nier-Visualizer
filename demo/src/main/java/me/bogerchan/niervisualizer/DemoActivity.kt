package me.bogerchan.niervisualizer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import me.bogerchan.niervisualizer.renderer.IRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleBarRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleRenderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType1Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType2Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType3Renderer
import me.bogerchan.niervisualizer.renderer.line.LineRenderer

/**
 * Created by BogerChan on 2017/12/2.
 */
class DemoActivity : AppCompatActivity() {

    companion object {
        val REQUEST_CODE_PERMISSION_AUDIO_FOR_INIT = 1
        val REQUEST_CODE_PERMISSION_AUDIO_FOR_CHANGE_STYLE = 2
    }

    private val svWave by lazy { findViewById<SurfaceView>(R.id.sv_wave) }
    private var mVisualizerManager: NierVisualizerManager? = null
    private val tvChangeStyle by lazy { findViewById<TextView>(R.id.tv_change_style) }
    private val mRenderers = arrayOf<Array<IRenderer>>(arrayOf(ColumnarType1Renderer()),
            arrayOf(ColumnarType2Renderer()),
            arrayOf(ColumnarType3Renderer()),
            arrayOf(LineRenderer(true)),
            arrayOf(CircleBarRenderer(2, true)),
            arrayOf(CircleRenderer(true)),
            arrayOf(CircleRenderer(true), CircleBarRenderer(2, true), ColumnarType2Renderer()),
            arrayOf(CircleRenderer(true), CircleBarRenderer(2, true), LineRenderer(true)))
    private var mCurrentStyleIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.colorPrimaryDark, theme)
        }
        supportActionBar?.hide()
        setContentView(R.layout.layout_demo)
        svWave.setZOrderOnTop(true)
        svWave.holder.setFormat(PixelFormat.TRANSLUCENT)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_CODE_PERMISSION_AUDIO_FOR_INIT)
        } else {
            mVisualizerManager = NierVisualizerManager()
            mVisualizerManager?.init(0)
        }
        tvChangeStyle.setOnClickListener {
            changeStyle()
        }
    }

    private fun changeStyle() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_CODE_PERMISSION_AUDIO_FOR_CHANGE_STYLE)
        } else {
            useStyle(++mCurrentStyleIndex)
        }
    }

    private fun useStyle(idx: Int) {
        if (mVisualizerManager == null) {
            val nvm = NierVisualizerManager()
            nvm.init(0)
            mVisualizerManager = nvm
        }
        mVisualizerManager?.start(svWave, mRenderers[idx % mRenderers.size])
    }

    override fun onStart() {
        super.onStart()
        changeStyle()
    }

    override fun onStop() {
        super.onStop()
        mVisualizerManager?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mVisualizerManager?.release()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.isEmpty()) {
            return
        }
        when (requestCode) {
            REQUEST_CODE_PERMISSION_AUDIO_FOR_INIT -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(findViewById<View>(android.R.id.content),
                            "Please enable AUDIO RECORD permission!",
                            Snackbar.LENGTH_SHORT)
                            .show()
                } else {
                    mVisualizerManager = NierVisualizerManager()
                    mVisualizerManager?.init(0)
                    useStyle(++mCurrentStyleIndex)
                }
            }
            REQUEST_CODE_PERMISSION_AUDIO_FOR_CHANGE_STYLE -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(findViewById<View>(android.R.id.content),
                            "Please enable AUDIO RECORD permission!",
                            Snackbar.LENGTH_SHORT)
                            .show()
                } else {
                    useStyle(++mCurrentStyleIndex)
                }
            }
        }
    }
}