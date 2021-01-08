package org.rfcx.guardian.classify.utils

import android.content.Context
import android.util.Log
import org.rfcx.guardian.classify.RfcxGuardian
import org.rfcx.guardian.classify.utils.AudioConverter.pickBetween
import org.rfcx.guardian.classify.model.MLPredictor
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.io.File
import kotlin.math.roundToInt

class AudioClassifyUtils(context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyUtils")

    private val app: RfcxGuardian = context.applicationContext as RfcxGuardian

    private var windowLength: Int = -1
    private var startAt: Int = -1
    private var endAt: Int = -1
    private var stepSize: Int = -1

    /**
     * Function to init all the attributes
     * Need to be called before classify an audio
     * Also used when related prefs have been changed and they need to be re-assigned
     */
    fun initClassifierAttributes(sampleRate: Int, windowSize: Float, step: Float) {
        this.windowLength = (sampleRate * windowSize).roundToInt()
        this.startAt = 0
        this.endAt = this.windowLength
        this.stepSize = (this.windowLength * step).roundToInt()
    }

    /**
     * Check if all attributes have been assigned
     */
    private fun isClassifierReady(): Boolean {
        return this.windowLength != -1 && this.startAt != -1 && this.endAt != -1 && this.stepSize != -1
    }

    /**
     * Move to the next chunk of audio based on window length and step size
     */
    private fun nextAudio() {
        this.startAt = this.startAt + this.stepSize
        this.endAt = this.startAt + this.windowLength
    }

    /**
     * Reset the attributes after entire audio has been classified
     */
    private fun resetStartAndEnd() {
        this.startAt = 0
        this.endAt = this.windowLength
    }

    fun classifyAudio(file: File) {
        val path = file.absolutePath
        classifyAudio(path)
    }

    fun classifyAudio(path: String) {
        val audio = AudioConverter.readAudioSimple(path)
        //check if all attributes are set and audio picked not more than its size.
        while (isClassifierReady() && (this.startAt + this.windowLength) < audio.size) {
            app.mlPredictor.load()
            val output = app.mlPredictor.run(audio.pickBetween(this.startAt, this.endAt))
            Log.d(logTag, "${output[0]} ${output[1]} ${output[2]}")
            nextAudio()
        }
        //reset after audio classified
        resetStartAndEnd()
    }

    fun getOutputAsList(): List<String> {
        val outputString = app.rfcxPrefs.getPrefAsString("prediction_model_output")
        return outputString.split(",")
    }

    fun getOutputSize(): Int {
        return getOutputAsList().size
    }
}
