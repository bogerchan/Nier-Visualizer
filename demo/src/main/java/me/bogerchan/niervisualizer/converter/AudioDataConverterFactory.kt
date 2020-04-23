package me.bogerchan.niervisualizer.converter

import android.media.AudioFormat
import android.media.AudioRecord

object AudioDataConverterFactory {

    fun getConverterByAudioRecord(audioRecord: AudioRecord) =
        when (audioRecord.audioFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> PCM8BitAudioDataConverter(audioRecord)
            AudioFormat.ENCODING_PCM_16BIT -> PCM16BitAudioDataConverter(audioRecord)
            else -> {
                throw UnsupportedOperationException("The audio format doesn't support now, audioFormat: ${audioRecord.audioFormat}")
            }
        }

}