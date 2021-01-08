package org.rfcx.guardian.classify.model

import org.rfcx.guardian.classify.RfcxGuardian
import org.rfcx.guardian.classify.utils.AudioConverter
import org.rfcx.guardian.classify.utils.AudioConverter.pickBetween
import org.rfcx.guardian.utility.rfcx.RfcxLog
import kotlin.math.roundToInt

class AudioClassifier(private val sampleRate: Int, private val windowSize: Float, private val step: Float, private val outputList: List<String>) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifier")

    private var windowLength: Int = (this.sampleRate * this.windowSize).roundToInt()
    private var startAt: Int = 0
    private var endAt: Int = this.windowLength
    private var stepSize: Int = (this.windowLength * this.step).roundToInt()

    private var predictor = MLPredictor(this.outputList.size)

    /**
     * Move to the next chunk of audio based on window length and step size
     */
    private fun nextWindow() {
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

    fun classify(path: String): List<FloatArray> {
        val audio = AudioConverter.readAudioSimple(path)
        val outputs = arrayListOf<FloatArray>()
        //check if all attributes are set and audio picked not more than its size.
        while ((this.startAt + this.windowLength) < audio.size) {
            predictor.load()
            val output = predictor.run(audio.pickBetween(this.startAt, this.endAt))
            outputs.add(output)
            nextWindow()
        }
        //reset after audio classified
        resetStartAndEnd()
        return outputs
    }
}
