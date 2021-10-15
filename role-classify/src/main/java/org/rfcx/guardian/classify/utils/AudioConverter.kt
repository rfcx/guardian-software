package org.rfcx.guardian.classify.utils

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder


object AudioConverter {

    fun readAudioSimple(path: String): FloatArray {
        val input =
            BufferedInputStream(FileInputStream(path))
        val buff = ByteArray(File(path).length().toInt())
        val dis = DataInputStream(input)
        dis.readFully(buff)
        // remove wav header at first 44 bytes
        return floatMe(shortMe(buff.sliceArray(44 until buff.size)) ?: ShortArray(0)) ?: FloatArray(
            0
        )
    }

    private fun shortMe(bytes: ByteArray): ShortArray? {
        val out = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out)
        return out
    }

    private fun floatMe(pcms: ShortArray): FloatArray? {
        val floats = FloatArray(pcms.size)
        pcms.forEachIndexed { index, sh ->
            // float to -1,+1
            floats[index] = sh.toFloat() / 32768.0f
        }
        return floats
    }


    /**
     * To slide an audio to windows of (sample rate * window size) length
     * parameters:
     * step: integer = 1 (seconds)
     * windowSize: float = 0.975
     * output:
     * [[0.9, 0.1], [0.8, 0.2]]
     */
    fun FloatArray.slidingWindow(
        sampleRate: Int,
        step: Float,
        windowSize: Float
    ): List<FloatArray> {
        val slicedAudio = arrayListOf<FloatArray>()
        val windowLength = (sampleRate * windowSize).toInt()
        var startAt = 0
        var endAt = windowLength
        val stepSize = (windowLength * step).toInt()
        while ((startAt + windowLength) < this.size) {
            slicedAudio.add(this.pickBetween(startAt, endAt))
            startAt += stepSize
            endAt = startAt + windowLength
        }
        return slicedAudio
    }

    fun FloatArray.pickBetween(startAt: Int, endAt: Int): FloatArray {
        return this.copyOfRange(startAt, endAt)
    }
}
