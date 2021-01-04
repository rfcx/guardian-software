package org.rfcx.guardian.guardian.audio.classify

import android.content.Context
import android.util.Log
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.audio.classify.AudioConverter.sliceTo
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.io.File

class AudioClassifyUtils(context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyUtils")

    private val app: RfcxGuardian = context.applicationContext as RfcxGuardian

    fun classifyAudio(file: File) {
        val step = app.rfcxPrefs.getPrefAsInt("prediction_step_size")
        val path = file.absolutePath
        MLPredictor().also {
            it.load()
            AudioConverter.readAudioSimple(path).sliceTo(0).forEach { audioChunk ->
                if (audioChunk.size == 15600) {
                    val output = it.run(audioChunk)
                    Log.d(logTag, output)
                }
            }
        }
    }
}
