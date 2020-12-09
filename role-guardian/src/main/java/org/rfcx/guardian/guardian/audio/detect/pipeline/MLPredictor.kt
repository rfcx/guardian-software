package org.rfcx.guardian.guardian.audio.detect.pipeline

import android.content.Context
import android.os.Environment
import android.util.Log
import org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces.Predictor
import org.tensorflow.lite.Interpreter
import java.io.File
import java.lang.Exception

/**
 * A predictor for tflite models (based on MLKit)
 */
class MLPredictor: Predictor {

    private var interpreter: Interpreter? = null

    override val isLoaded: Boolean
        get() = interpreter != null

    override fun load(context: Context) {
        if (interpreter != null) return

        try {
            interpreter = Interpreter(File(Environment.getExternalStorageDirectory(), "yamnet.tflite"))
        } catch (e: Exception) {
            Log.e("Rfcx", e.message)
        }
    }

    override fun run(input: FloatArray) {
        if (interpreter == null) {
            return
        }

        val input1: Array<FloatArray> = arrayOf(FloatArray(15600)) // Wrap in batch dim
        val outputShape: Array<FloatArray> = arrayOf(FloatArray(521))
        val input2: List<Float> = listOf(1f, 15600f)
        Log.d("Rfcx", input.size.toString())
        try {
            interpreter?.run(arrayOf(input.toSmallChunk(15600)), outputShape)
        } catch (e: Exception) {
            Log.e("Rfcx", e.message)
        }

        outputShape[0].filter { it != 0f }.forEach {
            Log.d("Rfcx", it.toString())
        }
    }

    private fun FloatArray.toSmallChunk(number: Int): FloatArray {
        return this.copyOfRange(0, number)
    }

}
