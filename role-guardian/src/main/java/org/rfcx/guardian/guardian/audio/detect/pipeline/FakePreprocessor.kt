package org.rfcx.guardian.guardian.audio.detect.pipeline

import org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces.Preprocessor
import kotlin.math.min

/**
 * An example preprocessor - do not use this class!
 */
class FakePreprocessor(
    private val inputSize: Int,
    private val outputSize: Int): Preprocessor {

    override fun run(input: FloatArray): FloatArray {
        val step =  inputSize.toDouble() / outputSize.toDouble()
        val inputList = input.toList()
        val output = FloatArray(outputSize)
        for (i in 0 until outputSize) {
            val start = min(inputSize-1, (i * step).toInt())
            val end = min(inputSize-1, ((i+1) * step).toInt())
            val inputSubset = inputList.subList(start, end)
            output[i] = inputSubset.averageIgnoreNaN()
        }
        return output
    }
}

fun Iterable<Float>.averageIgnoreNaN(): Float {
    var sum: Double = 0.0
    var count: Int = 0
    for (element in this) {
        if (element.isNaN()) continue
        sum += element
        count += 1
    }
    return if (count == 0) 0f else (sum / count).toFloat()
}
