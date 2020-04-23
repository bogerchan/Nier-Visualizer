package me.bogerchan.niervisualizer.converter

import android.media.AudioRecord
import android.util.Log
import kotlin.math.abs

class PCM8BitAudioDataConverter(audioRecord: AudioRecord) :
    AbsAudioDataConverter(audioRecord) {

    private val audioRecordByteBuffer: ByteArray = ByteArray(minBufferSize / 2)
    private val audioLength = (audioRecordByteBuffer.size * 1000F / audioRecord.sampleRate).toInt()

    private fun fixWave(rawData: Byte): Byte {
        if (abs((rawData.toInt() and 0xFF) - 127) < 5) {
            return rawData
        }
        return (((rawData.toInt() and 0xFF) - 127) * 5 + 127).toByte()
    }

    override fun convertWaveDataTo(buffer: ByteArray) {
        audioRecordByteBuffer.fill(0)
        if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            return
        }
        audioRecord.read(audioRecordByteBuffer, 0, audioRecordByteBuffer.size)
        var tempCounter = 0
        for (idx in audioRecordByteBuffer.indices step (audioRecordByteBuffer.size / (audioLength + buffer.size))) {
            if (tempCounter >= buffer.size) {
                break
            }
            buffer[tempCounter++] = fixWave(audioRecordByteBuffer[idx])
        }
    }

}