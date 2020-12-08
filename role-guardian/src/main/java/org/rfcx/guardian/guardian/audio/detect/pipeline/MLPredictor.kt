package org.rfcx.guardian.guardian.audio.detect.pipeline

import android.util.Log
import org.rfcx.guardian.guardian.audio.detect.pipeline.entities.Model
import org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces.Predictor
import org.rfcx.guardian.guardian.entity.Err
import org.rfcx.guardian.guardian.entity.Ok
import org.rfcx.guardian.guardian.entity.Result
import java.io.File

/**
 * A predictor for tflite models (based on MLKit)
 */
class MLPredictor: Predictor {

    private var interpreter: FirebaseModelInterpreter? = null
    private var inputOutputOptions: FirebaseModelInputOutputOptions? = null

    override val isLoaded: Boolean
        get() = interpreter != null

    override fun load(model: Model) {
        if (interpreter != null) return

        val modelPath = model.filePath
        if (! File(modelPath).exists()) {
            Log.e("MLPredictor", "Model file not found")
            return
        }

        inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
            .setInputFormat(0, FirebaseModelDataType.FLOAT32, Model.inputShape.toIntArray())
            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, Model.outputShape.toIntArray())
            .build()

        val localModel = FirebaseCustomLocalModel.Builder().setFilePath(modelPath).build()
        val interpreterOptions = FirebaseModelInterpreterOptions.Builder(localModel).build()
        interpreter = FirebaseModelInterpreter.getInstance(interpreterOptions)
        interpreter?.isStatsCollectionEnabled = true
    }

    override fun run(input: FloatArray, callback: (Result<FloatArray, Exception>) -> Unit) {
        if (interpreter == null) {
            callback(Err(Exception("Interpreter must be prepared before run")))
            return
        }

        val input1: Array<ByteArray> = arrayOf(input) // Wrap in batch dim
        val inputs = FirebaseModelInputs.Builder().add(input1).build()
        interpreter?.let {
            it.run(inputs, inputOutputOptions!!)
                .addOnSuccessListener { result ->
                    val outputs = result.getOutput<Array<FloatArray>>(0)
                    callback(Ok(outputs[0]))
                }
                .addOnFailureListener { e ->
                    callback(Err(e))
                }
        }
    }


}
