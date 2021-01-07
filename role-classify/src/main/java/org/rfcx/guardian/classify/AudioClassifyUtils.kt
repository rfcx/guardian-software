package org.rfcx.guardian.classify

import android.content.Context
import android.text.TextUtils
import android.util.Log
import org.rfcx.guardian.classify.AudioConverter.slidingWindow
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
        val sampleRate = 12000
        val windowSize = 0.975f
        val step = 1f
        val windowLength = (sampleRate * windowSize).toInt()
        var startAt = 0
        var endAt = windowLength
        val stepSize =  (windowLength * step).toInt()

        val audio = AudioConverter.readAudioSimple(path)
        while ((startAt + windowLength) < audio.size) {
            predictor.load()
            val output = predictor.run(audio.copyOfRange(startAt, endAt))
            Log.d(logTag, "${output[0]} ${output[1]} ${output[2]}")

            startAt += stepSize
            endAt = startAt + windowLength
        }
    }

    companion object {

    }
}
