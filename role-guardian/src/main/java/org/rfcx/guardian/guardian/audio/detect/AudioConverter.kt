package org.rfcx.guardian.guardian.audio.detect

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

        return floatMe(shortMe(buff.sliceArray(44 until buff.size)) ?: ShortArray(0)) ?: FloatArray(0)
    }

    private fun shortMe(bytes: ByteArray): ShortArray? {
        val out = ShortArray(bytes.size / 2) // will drop last byte if odd number
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out)
        return out
    }

    private fun floatMe(pcms: ShortArray): FloatArray? {
        val floats = FloatArray(pcms.size)
        pcms.forEachIndexed { index, sh ->
            floats[index] = sh.toFloat() / 32768.0f
        }
        return floats
    }
}
