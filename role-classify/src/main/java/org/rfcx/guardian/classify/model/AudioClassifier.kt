package org.rfcx.guardian.classify.model

import org.rfcx.guardian.audio.wav.WavWriter
import org.rfcx.guardian.classify.RfcxGuardian
import org.rfcx.guardian.classify.utils.AudioConverter
import org.rfcx.guardian.classify.utils.AudioConverter.pickBetween
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils
import org.rfcx.guardian.utility.misc.FileUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.io.File
import kotlin.math.roundToInt

class AudioClassifier(private val tfLiteFilePath: String, private val sampleRate: Int, private val windowSize: Float, private val step: Float, private val outputList: List<String>) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifier")

    private var windowLength: Int = (this.sampleRate * this.windowSize).roundToInt()
    private var startAt: Int = 0
    private var endAt: Int = this.windowLength
    private var stepSize: Int = (this.windowLength * this.step).roundToInt()

    private var predictor = MLPredictor(this.tfLiteFilePath, this.outputList.size)

    fun loadClassifier() {
        predictor.load()
    }

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
        var index = 0
        //check if all attributes are set and audio picked not more than its size.
        while ((this.startAt + this.windowLength) < audio.size) {
            predictor.load()
            val buffer = audio.pickBetween(this.startAt, this.endAt)
            val output = predictor.run(buffer)
            outputs.add(output)
            createSnippet(output, buffer, getFileName(path), index)
            index++
            nextWindow()
        }
        //reset after audio classified
        resetStartAndEnd()
        return outputs
    }

    private fun createSnippet(output: FloatArray, buffer: FloatArray, outputName: String, index: Int) {
        val isMostDetectionPassThreshold = output.max() ?: 0f > 0.9
        val indexOfMostDetection = output.indexOf(output.max() ?: 0f)
        var detection = ""
        if (isMostDetectionPassThreshold) {
            when (indexOfMostDetection) {
                0 -> detection = "chainsaw"
                1 -> detection = "gunshot"
                2 -> detection = "vehicle"
            }
            WavWriter.createSnippet(AudioConverter.doubleMe(buffer), this.sampleRate, "${outputName}_${index}_${detection}.wav")
        }
    }

    private fun getFileName(path: String): String {
        return File(path).nameWithoutExtension
    }
}
