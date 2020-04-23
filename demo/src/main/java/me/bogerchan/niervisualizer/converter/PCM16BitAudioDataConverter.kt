package me.bogerchan.niervisualizer.converter

import android.media.AudioRecord
import kotlin.math.abs

class PCM16BitAudioDataConverter(audioRecord: AudioRecord) :
    AbsAudioDataConverter(audioRecord) {

    private val audioRecordShortBuffer: ShortArray = ShortArray(minBufferSize / 2)
    private val audioLength = (audioRecordShortBuffer.size * 1000F / audioRecord.sampleRate).toInt()

    private fun fixWave(rawData: Short): Byte {
        val scaledData = ((rawData.toInt() and 0xFFFF) - 0x7FFF) / 256
        if (abs(abs(scaledData) - 127) < 5) {
            return 127
        }
        return ((scaledData - 127) * 5 + 127).toByte()
    }

    override fun convertWaveDataTo(buffer: ByteArray) {
        audioRecordShortBuffer.fill(0)
        if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            return
        }
        audioRecord.read(audioRecordShortBuffer, 0, audioRecordShortBuffer.size)
        var tempCounter = 0
        for (idx in audioRecordShortBuffer.indices step (audioRecordShortBuffer.size / (audioLength + buffer.size))) {
            if (tempCounter >= buffer.size) {
                break
            }
            buffer[tempCounter++] = fixWave(audioRecordShortBuffer[idx])
        }
    }

}