package org.rfcx.guardian.classify

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
        return floatMe(shortMe(buff.sliceArray(44 until buff.size)) ?: ShortArray(0)) ?: FloatArray(0)
    }

    fun FloatArray.slidingWindow(sampleRate: Int, step: Int, windowSize: Float): List<FloatArray> {
        val slicedAudio = arrayListOf<FloatArray>()
        val chunkSize = (sampleRate * windowSize).toInt()
        var startAt = 0
        var endAt = chunkSize
        val stepSize =  if (step != 0) (chunkSize * (1f / (2 * step))).toInt() else 0
        while ((startAt + chunkSize) < this.size) {
            if (startAt != 0) {
                startAt = endAt - stepSize
                endAt = startAt + chunkSize
            }
            slicedAudio.add(this.copyOfRange(startAt, endAt))
            startAt = endAt
        }
        return slicedAudio
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
            floats[index] = sh.toFloat() / Float.MAX_VALUE
        }
        return floats
    }
}
