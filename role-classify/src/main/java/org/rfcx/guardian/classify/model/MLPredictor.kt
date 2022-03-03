package org.rfcx.guardian.classify.model

import android.content.Context
import android.os.Environment
import android.util.Log
import org.rfcx.guardian.classify.RfcxGuardian
import org.rfcx.guardian.classify.model.interfaces.Predictor
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.tensorflow.lite.Interpreter
import java.io.File

class MLPredictor(private val tfLiteFilePath: String, private val outputSize: Int): Predictor {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "MLPredictor")

    private var interpreter: Interpreter? = null

    override val isLoaded: Boolean
        get() = interpreter != null

    override fun load() {
        if (interpreter != null) return

        try {
            interpreter = Interpreter( File( tfLiteFilePath ) )
        } catch (e: Exception) {
            Log.e(logTag, e.message)
        }
    }

    override fun run(input: FloatArray): FloatArray {
        if (interpreter == null) {
            return FloatArray(0)
        }

        val outputShape: Array<FloatArray> = arrayOf(FloatArray(this.outputSize))
        try {
            interpreter?.run(arrayOf(input), outputShape)
        } catch (e: Exception) {
            Log.e(logTag, e.message)
        }

        return outputShape[0]
    }

}
