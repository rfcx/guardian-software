package org.rfcx.guardian.classify.model

import android.util.Log
import org.rfcx.guardian.classify.RfcxGuardian
import org.rfcx.guardian.classify.utils.AudioConverter
import org.rfcx.guardian.classify.utils.AudioConverter.pickBetween
import org.rfcx.guardian.utility.misc.DateTimeUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog
import kotlin.math.roundToInt

class AudioClassifier(private val tfLiteFilePath: String, private val sampleRate: Int, private val windowSizeSecs: Float, private val stepSizeSecs: Float, private val outputList: List<String>) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifier")

    private var windowLengthSamples: Int = (this.sampleRate * this.windowSizeSecs).roundToInt()
    private var startAt: Int = 0
    private var endAt: Int = this.windowLengthSamples
    private var stepLengthSamples: Int = (this.sampleRate * this.stepSizeSecs).roundToInt()

    private var predictor = MLPredictor(this.tfLiteFilePath, this.outputList.size)

    fun loadClassifier() {
        predictor.load()
    }

    /**
     * Move to the next chunk of audio based on window length and step size
     */
    private fun nextWindow() {
        this.startAt = this.startAt + this.stepLengthSamples
        this.endAt = this.startAt + this.windowLengthSamples
    }

    /**
     * Reset the attributes after entire audio has been classified
     */
    private fun resetStartAndEnd() {
        this.startAt = 0
        this.endAt = this.windowLengthSamples
    }

    fun classify(path: String, verboseLogging: Boolean): List<FloatArray> {
        val audio = AudioConverter.readAudioSimple(path)
        val outputs = arrayListOf<FloatArray>()
        //check if all attributes are set and audio picked not more than its size.
        while ((this.startAt + this.windowLengthSamples) < audio.size) {
            predictor.load()
            val iterationStartTime = System.currentTimeMillis()
            val output = predictor.run(audio.pickBetween(this.startAt, this.endAt))
            outputs.add(output)

            if (verboseLogging) {
                Log.e(
                    this.logTag,
                    "Processed " + (this.startAt / this.sampleRate) + " to " + (this.endAt / this.sampleRate) + " secs (" + (this.endAt - this.startAt) + " samples) of " + (audio.size / this.sampleRate) + " secs (processing required " + DateTimeUtils.timeStampDifferenceFromNowAsReadableString(
                        iterationStartTime
                    ) + ")"
                )
            }

            nextWindow()
        }
        //reset after audio classified
        resetStartAndEnd()
        return outputs
    }
}
