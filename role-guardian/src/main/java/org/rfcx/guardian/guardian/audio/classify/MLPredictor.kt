package org.rfcx.guardian.guardian.audio.classify

import android.os.Environment
import android.util.Log
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.audio.classify.interfaces.Predictor
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.tensorflow.lite.Interpreter
import java.io.File

class MLPredictor: Predictor {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "MLPredictor")

    private var interpreter: Interpreter? = null

    override val isLoaded: Boolean
        get() = interpreter != null

    override fun load() {
        if (interpreter != null) return

        try {
            interpreter = Interpreter(
                File(
                    Environment.getExternalStorageDirectory(),
                    "yamnet.tflite"
                )
            )
        } catch (e: Exception) {
            Log.e(logTag, e.message)
        }
    }

    override fun run(input: FloatArray): String {
        if (interpreter == null) {
            return ""
        }
        // fix output size to 521
        val outputShape: Array<FloatArray> = arrayOf(FloatArray(521))
        try {
            interpreter?.run(arrayOf(input), outputShape)
        } catch (e: Exception) {
            Log.e(logTag, e.message)
        }

        val filteredOutput = arrayListOf<String>()
        outputShape[0].forEachIndexed { index, fl ->
            // pick only confidence more than 0.1
            if (fl != 0f && fl >= 0.1f) {
                filteredOutput.add("$index-$fl")
            }
        }

        return filteredOutput.joinToString("*")
    }

}
