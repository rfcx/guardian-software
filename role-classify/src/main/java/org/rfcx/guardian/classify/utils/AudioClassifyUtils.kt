package org.rfcx.guardian.classify.utils

import android.content.Context
import android.util.Log
import org.rfcx.guardian.classify.RfcxGuardian
import org.rfcx.guardian.classify.model.AudioClassifier
import org.rfcx.guardian.classify.utils.AudioConverter.pickBetween
import org.rfcx.guardian.classify.model.MLPredictor
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.io.File
import kotlin.math.roundToInt

class AudioClassifyUtils(context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyUtils")

    private val app: RfcxGuardian = context.applicationContext as RfcxGuardian

    private var classifier: AudioClassifier? = null

    /**
     * Function to init all the attributes
     * Need to be called before classify an audio
     * Also used when related prefs have been changed and they need to be re-assigned
     */
    fun initClassifier(sampleRate: Int, windowSize: Float, step: Float) {
        this.classifier = AudioClassifier(sampleRate, windowSize, step, getOutputAsList())
    }

    fun classifyAudio(file: File) {
        val path = file.absolutePath
        classifyAudio(path)
    }

    fun classifyAudio(path: String): List<FloatArray> {
        return this.classifier?.classify(path) ?: listOf()
    }

    fun getOutputAsList(): List<String> {
        val outputString = app.rfcxPrefs.getPrefAsString("prediction_model_output")
        return outputString.split(",")
    }

    fun getOutputSize(): Int {
        return getOutputAsList().size
    }
}
