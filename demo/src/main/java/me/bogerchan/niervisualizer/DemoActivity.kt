package me.bogerchan.niervisualizer

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import me.bogerchan.niervisualizer.converter.AbsAudioDataConverter
import me.bogerchan.niervisualizer.converter.AudioDataConverterFactory
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
        const val REQUEST_CODE_AUDIO_PERMISSION = 1
        const val STATE_PLAYING = 0
        const val STATE_PAUSE = 1
        const val STATE_STOP = 2

        const val STATUS_UNKNOWN = 0
        const val STATUS_AUDIO_RECORD = 1
        const val STATUS_MEDIA_PLAYER = 2

        const val SAMPLING_RATE = 44100
        const val AUDIO_RECORD_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_RECORD_FORMAT =  AudioFormat.ENCODING_PCM_16BIT
    }

    private val svWave by lazy { findViewById<SurfaceView>(R.id.sv_wave) }
    private var mVisualizerManager: NierVisualizerManager? = null
    private val tvChangeStyle by lazy { findViewById<TextView>(R.id.tv_change_style) }
    private val tvMediaPlayerStartOrStop by lazy { findViewById<TextView>(R.id.tv_media_player_start_or_stop) }
    private val tvMediaPlayerPauseOrResume by lazy { findViewById<TextView>(R.id.tv_media_player_pause_or_resume) }
    private val tvAudioRecordStartOrStop by lazy { findViewById<TextView>(R.id.tv_audio_record_start_or_stop) }
    private val mPlayer by lazy {
        MediaPlayer().apply {
            resources.openRawResourceFd(R.raw.demo_audio).apply {
                setDataSource(fileDescriptor, startOffset, length)
            }
        }
    }
    private val mAudioBufferSize by lazy {
        AudioRecord.getMinBufferSize(
            SAMPLING_RATE,
            AUDIO_RECORD_CHANNEL_CONFIG,
            AUDIO_RECORD_FORMAT
        )
    }
    private val mAudioRecord by lazy {
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLING_RATE,
            AUDIO_RECORD_CHANNEL_CONFIG,
            AUDIO_RECORD_FORMAT,
            mAudioBufferSize
        )
    }
    private val mRenderers = arrayOf<Array<IRenderer>>(
        arrayOf(ColumnarType1Renderer()),
        arrayOf(ColumnarType2Renderer()),
        arrayOf(ColumnarType3Renderer()),
        arrayOf(ColumnarType4Renderer()),
        arrayOf(LineRenderer(true)),
        arrayOf(CircleBarRenderer()),
        arrayOf(CircleRenderer(true)),
        arrayOf(
            CircleRenderer(true),
            CircleBarRenderer(),
            ColumnarType4Renderer()
        ),
        arrayOf(CircleRenderer(true), CircleBarRenderer(), LineRenderer(true)),
        arrayOf(
            ArcStaticRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#cfa9d0fd")
                }),
            ArcStaticRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#dad2eafe")
                },
                amplificationOuter = .83f,
                startAngle = -90f,
                sweepAngle = 225f
            ),
            ArcStaticRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#7fa9d0fd")
                },
                amplificationOuter = .93f,
                amplificationInner = 0.8f,
                startAngle = -45f,
                sweepAngle = 135f
            ),
            CircleSolidRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#d2eafe")
                },
                amplification = .45f
            ),
            CircleBarRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 4f
                    color = Color.parseColor("#efe3f2ff")
                },
                modulationStrength = 1f,
                type = CircleBarRenderer.Type.TYPE_A_AND_TYPE_B,
                amplification = 1f, divisions = 8
            ),
            CircleBarRenderer(
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 5f
                    color = Color.parseColor("#e3f2ff")
                },
                modulationStrength = 0.1f,
                amplification = 1.2f,
                divisions = 8
            ),
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
                    values = floatArrayOf(0f, -360f)
                )
            ),
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
                    values = floatArrayOf(0f, -360f)
                )
            )
        )
    )
    private var mCurrentStyleIndex = 0
    private var mMediaPlayerState = STATE_STOP
    private var mAudioRecordState = STATE_STOP
    private var mStatus = STATUS_UNKNOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor =
                ResourcesCompat.getColor(resources, R.color.colorPrimaryDark, theme)
        }
        supportActionBar?.hide()
        setContentView(R.layout.layout_demo)
        svWave.setZOrderOnTop(true)
        svWave.holder.setFormat(PixelFormat.TRANSLUCENT)
        tvChangeStyle.setOnClickListener {
            changeStyle()
        }
        tvMediaPlayerStartOrStop.setOnClickListener {
            mPlayer.apply {
                when (mMediaPlayerState) {
                    STATE_PLAYING -> {
                        stop()
                        mMediaPlayerState = STATE_STOP
                        mVisualizerManager?.stop()
                        tvMediaPlayerPauseOrResume.isEnabled = false
                        tvMediaPlayerStartOrStop.text = "START"
                    }
                    STATE_STOP -> {
                        prepare()
                        start()
                        mMediaPlayerState = STATE_PLAYING
                        if (mStatus == STATUS_AUDIO_RECORD || mStatus == STATUS_UNKNOWN) {
                            mAudioRecord.stop()
                            tvAudioRecordStartOrStop.text = "START"
                            mAudioRecordState = STATE_STOP
                            mStatus = STATUS_MEDIA_PLAYER
                            createNewVisualizerManager()
                        }
                        useStyle(mCurrentStyleIndex)
                        tvMediaPlayerPauseOrResume.isEnabled = true
                        tvMediaPlayerStartOrStop.text = "STOP"
                    }
                    STATE_PAUSE -> {
                        stop()
                        prepare()
                        start()
                        mMediaPlayerState = STATE_PLAYING
                        useStyle(mCurrentStyleIndex)
                        tvMediaPlayerPauseOrResume.isEnabled = true
                        tvMediaPlayerStartOrStop.text = "STOP"
                    }
                }
                tvMediaPlayerPauseOrResume.text = "PAUSE"
            }
            mStatus = STATUS_MEDIA_PLAYER
        }
        tvMediaPlayerPauseOrResume.setOnClickListener {
            mPlayer.apply {
                when (mMediaPlayerState) {
                    STATE_PLAYING -> {
                        pause()
                        mMediaPlayerState = STATE_PAUSE
                        mVisualizerManager?.pause()
                        tvMediaPlayerPauseOrResume.text = "RESUME"
                    }
                    STATE_PAUSE -> {
                        start()
                        mMediaPlayerState = STATE_PLAYING
                        mVisualizerManager?.resume()
                        tvMediaPlayerPauseOrResume.text = "PAUSE"
                    }
                }
            }
        }
        tvAudioRecordStartOrStop.setOnClickListener {
            mAudioRecord.apply {
                when (mAudioRecordState) {
                    STATE_PLAYING -> {
                        stop()
                        mAudioRecordState = STATE_STOP
                        mVisualizerManager?.stop()
                        tvAudioRecordStartOrStop.text = "START"
                    }
                    STATE_STOP -> {
                        startRecording()
                        mAudioRecordState = STATE_PLAYING
                        if (mStatus == STATUS_MEDIA_PLAYER || mStatus == STATUS_UNKNOWN) {
                            mPlayer.stop()
                            tvMediaPlayerStartOrStop.text = "START"
                            tvMediaPlayerPauseOrResume.isEnabled = false
                            mMediaPlayerState = STATE_STOP
                            mStatus = STATUS_AUDIO_RECORD
                            createNewVisualizerManager()
                        }
                        useStyle(mCurrentStyleIndex)
                        tvAudioRecordStartOrStop.text = "STOP"
                    }
                }
            }
            mStatus = STATUS_AUDIO_RECORD
        }
        ensurePermissionAllowed()
    }

    private fun ensurePermissionAllowed() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_AUDIO_PERMISSION
            )
        }
    }

    private fun changeStyle() {
        useStyle(++mCurrentStyleIndex)
    }

    private fun useStyle(idx: Int) {
        mVisualizerManager?.start(svWave, mRenderers[idx % mRenderers.size])
    }

    private fun createNewVisualizerManager() {
        mVisualizerManager?.release()
        mVisualizerManager = NierVisualizerManager().apply {
            when (mStatus) {
                STATUS_MEDIA_PLAYER -> {
                    init(mPlayer.audioSessionId)
                }
                STATUS_AUDIO_RECORD -> {
                    init(object : NierVisualizerManager.NVDataSource {

                        private val mBuffer: ByteArray = ByteArray(512)
                        private val mAudioDataConverter: AbsAudioDataConverter =
                            AudioDataConverterFactory.getConverterByAudioRecord(mAudioRecord)

                        override fun getDataSamplingInterval() = 0L

                        override fun getDataLength() = mBuffer.size

                        override fun fetchFftData(): ByteArray? {
                            return null
                        }

                        override fun fetchWaveData(): ByteArray? {
                            mAudioDataConverter.convertWaveDataTo(mBuffer)
                            return mBuffer
                        }

                    })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mVisualizerManager?.apply {
            when (mStatus) {
                STATUS_MEDIA_PLAYER -> {
                    if (mPlayer.isPlaying) {
                        resume()
                    }
                }
                STATUS_AUDIO_RECORD -> {
                    if (mAudioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        resume()
                    }
                }
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
        mPlayer.release()
        mAudioRecord.release()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_AUDIO_PERMISSION -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Demo need record permission, please allow it to show this visualize effect!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }
}