package me.bogerchan.niervisualizer.converter

import android.media.AudioRecord

abstract class AbsAudioDataConverter(protected val audioRecord: AudioRecord) {

    protected val minBufferSize: Int by lazy {
        AudioRecord.getMinBufferSize(
            audioRecord.sampleRate,
            audioRecord.channelConfiguration,
            audioRecord.audioFormat
        )
    }

    abstract fun convertWaveDataTo(buffer: ByteArray)

}