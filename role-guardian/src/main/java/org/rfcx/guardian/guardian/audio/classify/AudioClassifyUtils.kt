package org.rfcx.guardian.guardian.audio.classify

import android.content.Context
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.audio.classify.AudioConverter.slidingWindow
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.io.File

class AudioClassifyUtils(context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyUtils")

    private val app: RfcxGuardian = context.applicationContext as RfcxGuardian

    private val predictor = MLPredictor()

    fun classifyAudio(file: File) {
        val path = file.absolutePath
        classifyAudio(path)
    }

    fun classifyAudio(path: String) {
        val step = app.rfcxPrefs.getPrefAsInt("prediction_step_size")
        val windowSize = app.rfcxPrefs.getPrefAsFloat("prediction_window_size")
        val finalStepSize = (step * windowSize).toInt()
        val detections = arrayListOf<FloatArray>()
        predictor.also {
            it.load()
            AudioConverter.readAudioSimple(path).slidingWindow(step,windowSize).forEach { audioChunk ->
                if (audioChunk.size == finalStepSize) {
                    val output = it.run(audioChunk)
                    detections.add(output)
                }
            }
        }
        //TODO: use detections on cognition
    }

    companion object {

    }
}
