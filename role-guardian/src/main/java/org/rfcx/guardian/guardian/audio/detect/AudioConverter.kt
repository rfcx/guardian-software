package org.rfcx.guardian.guardian.audio.detect

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

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
        return output.toByteArray()
    }
}
