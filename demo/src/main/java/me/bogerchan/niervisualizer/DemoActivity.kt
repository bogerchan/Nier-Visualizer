package me.bogerchan.niervisualizer

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import me.bogerchan.niervisualizer.renderer.IRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleBarRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleSolidRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleWaveRenderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType1Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType2Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType3Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType4Renderer
import me.bogerchan.niervisualizer.renderer.line.LineRenderer
import me.bogerchan.niervisualizer.renderer.other.ArcStaticRenderer
import me.bogerchan.niervisualizer.util.NierAnimator

/**
 * Created by BogerChan on 2017/12/2.
 */
class DemoActivity : AppCompatActivity() {

    companion object {
        val REQUEST_CODE_AUDIO_PERMISSION = 1
        val STATE_PLAYING = 0
        val STATE_PAUSE = 1
        val STATE_STOP = 2
    }

    private val svWave by lazy { findViewById<SurfaceView>(R.id.sv_wave) }
    private var mVisualizerManager: NierVisualizerManager? = null
    private val tvChangeStyle by lazy { findViewById<TextView>(R.id.tv_change_style) }
    private val tvStartOrStop by lazy { findViewById<TextView>(R.id.tv_start_or_stop) }
    private val tvPauseOrResume by lazy { findViewById<TextView>(R.id.tv_pause_or_resume) }
    private val mPlayer by lazy {
        MediaPlayer().apply {
            resources.openRawResourceFd(R.raw.demo_audio).apply {
                setDataSource(fileDescriptor, startOffset, length)
            }
        }
    }
    private val mRenderers = arrayOf<Array<IRenderer>>(
            arrayOf(ColumnarType1Renderer()),
            arrayOf(ColumnarType2Renderer()),
            arrayOf(ColumnarType3Renderer()),
            arrayOf(ColumnarType4Renderer()),
            arrayOf(LineRenderer(true)),
            arrayOf(CircleBarRenderer()),
            arrayOf(CircleRenderer(true)),
            arrayOf(CircleRenderer(true),
                    CircleBarRenderer(),
                    ColumnarType4Renderer()),
            arrayOf(CircleRenderer(true), CircleBarRenderer(), LineRenderer(true)),
            arrayOf(ArcStaticRenderer(
                    paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#cfa9d0fd")
                    }),
                    ArcStaticRenderer(
                            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.parseColor("#dad2eafe")
                            },
                            amplificationOuter = .83f,
                            startAngle = -90f,
                            sweepAngle = 225f),
                    ArcStaticRenderer(
                            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.parseColor("#7fa9d0fd")
                            },
                            amplificationOuter = .93f,
                            amplificationInner = 0.8f,
                            startAngle = -45f,
                            sweepAngle = 135f),
                    CircleSolidRenderer(
                            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.parseColor("#d2eafe")
                            },
                            amplification = .45f),
                    CircleBarRenderer(
                            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                strokeWidth = 4f
                                color = Color.parseColor("#efe3f2ff")
                            },
                            modulationStrength = 1f,
                            type = CircleBarRenderer.Type.TYPE_A_AND_TYPE_B,
                            amplification = 1f, divisions = 8),
                    CircleBarRenderer(
                            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                strokeWidth = 5f
                                color = Color.parseColor("#e3f2ff")
                            },
                            modulationStrength = 0.1f,
                            amplification = 1.2f,
                            divisions = 8),
                    CircleWaveRenderer(
                            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                strokeWidth = 6f
                                color = Color.WHITE
                            },
                            modulationStrength = 0.2f,
                            type = CircleWaveRenderer.Type.TYPE_B,
                            amplification = 1f,
                            animator = NierAnimator(
                                    interpolator = LinearInterpolator(),
                                    duration = 20000,
                                    values = floatArrayOf(0f, -360f))),
                    CircleWaveRenderer(
                            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                strokeWidth = 6f
                                color = Color.parseColor("#7fcee7fe")
                            },
                            modulationStrength = 0.2f,
                            type = CircleWaveRenderer.Type.TYPE_B,
                            amplification = 1f,
                            divisions = 8,
                            animator = NierAnimator(
                                    interpolator = LinearInterpolator(),
                                    duration = 20000,
                                    values = floatArrayOf(0f, -360f))))
    )
    private var mCurrentStyleIndex = 0
    private var mPlayerState = STATE_STOP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.colorPrimaryDark, theme)
        }
        supportActionBar?.hide()
        setContentView(R.layout.layout_demo)
        svWave.setZOrderOnTop(true)
        svWave.holder.setFormat(PixelFormat.TRANSLUCENT)
        tvChangeStyle.setOnClickListener {
            changeStyle()
        }
        tvStartOrStop.setOnClickListener {
            mPlayer.apply {
                when (mPlayerState) {
                    STATE_PLAYING -> {
                        stop()
                        mPlayerState = STATE_STOP
                        mVisualizerManager?.stop()
                        tvPauseOrResume.isEnabled = false
                        tvStartOrStop.text = "START"
                    }
                    STATE_STOP -> {
                        prepare()
                        start()
                        mPlayerState = STATE_PLAYING
                        useStyle(mCurrentStyleIndex)
                        tvPauseOrResume.isEnabled = true
                        tvStartOrStop.text = "STOP"
                    }
                    STATE_PAUSE -> {
                        stop()
                        prepare()
                        start()
                        mPlayerState = STATE_PLAYING
                        useStyle(mCurrentStyleIndex)
                        tvPauseOrResume.isEnabled = true
                        tvStartOrStop.text = "STOP"
                    }
                }
                tvPauseOrResume.text = "PAUSE"
            }
        }
        tvPauseOrResume.setOnClickListener {
            mPlayer.apply {
                when (mPlayerState) {
                    STATE_PLAYING -> {
                        pause()
                        mPlayerState = STATE_PAUSE
                        mVisualizerManager?.pause()
                        tvPauseOrResume.text = "RESUME"
                    }
                    STATE_PAUSE -> {
                        start()
                        mPlayerState = STATE_PLAYING
                        mVisualizerManager?.resume()
                        tvPauseOrResume.text = "PAUSE"
                    }
                }
            }
        }
        ensurePermissionAllowed()
    }

    private fun ensurePermissionAllowed() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_CODE_AUDIO_PERMISSION)
        }
    }

    private fun changeStyle() {
        useStyle(++mCurrentStyleIndex)
    }

    private fun useStyle(idx: Int) {
        if (mVisualizerManager == null) {
            val nvm = NierVisualizerManager()
            nvm.init(mPlayer.audioSessionId)
            mVisualizerManager = nvm
        }
        mVisualizerManager?.start(svWave, mRenderers[idx % mRenderers.size])
    }

    override fun onStart() {
        super.onStart()
        mVisualizerManager?.apply {
            if (mPlayer.isPlaying) {
                resume()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mVisualizerManager?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mVisualizerManager?.release()
        mVisualizerManager = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_AUDIO_PERMISSION -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Demo need record permission, please allow it to show this visualize effect!", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
}