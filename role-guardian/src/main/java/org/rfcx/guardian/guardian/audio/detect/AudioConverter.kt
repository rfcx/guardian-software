package org.rfcx.guardian.guardian.audio.detect

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer

object AudioConverter {
    fun readAudioSimple(path: String): FloatArray {
        val output = ByteArrayOutputStream()
        val input =
            BufferedInputStream(FileInputStream(path))

        var read: Int
        val buff = ByteArray(1024)
        while (input.read(buff).also { read = it } > 0) {
            output.write(buff, 0, read)
        }
        output.flush()
        return floatMe(shortMe(output.toByteArray()) ?: ShortArray(0)) ?: FloatArray(0)
    }

    private fun shortMe(bytes: ByteArray): ShortArray? {
        val out = ShortArray(bytes.size / 2) // will drop last byte if odd number
        val bb: ByteBuffer = ByteBuffer.wrap(bytes)
        for (i in out.indices) {
            out[i] = bb.short
        }
        return out
    }

    private fun floatMe(pcms: ShortArray): FloatArray? {
        val floaters = FloatArray(pcms.size)
        for (i in pcms.indices) {
            floaters[i] = pcms[i].toFloat()
        }
        return floaters
    }
}
