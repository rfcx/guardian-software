package org.rfcx.guardian.guardian.audio.detect.pipeline

import org.rfcx.guardian.guardian.audio.detect.pipeline.entities.LeakConfidence
import org.rfcx.guardian.guardian.audio.detect.pipeline.entities.Model
import org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces.Pipeline
import org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces.Postprocessor
import org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces.Predictor
import org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces.Preprocessor
import org.rfcx.guardian.guardian.entity.Err
import org.rfcx.guardian.guardian.entity.Ok
import org.rfcx.guardian.guardian.entity.Result

/**
 * An example pipeline with a fake preprocessing step
 */

class ExamplePipeline: Pipeline {

    private val windowSamples: Int
    private val model: Model
    private val preprocessor: Preprocessor
    private val predictor: Predictor
    private val postprocessor: Postprocessor

    constructor(windowSamples: Int, model: Model) {
        this.windowSamples = windowSamples
        this.model = model
        preprocessor = FakePreprocessor(windowSamples, Model.inputShape[1])
        predictor = MLPredictor()
        postprocessor = SimplePostprocessor(Model.outputIndexOfLeak)
    }

    override val isReady: Boolean
        get() = predictor.isLoaded

    override fun prepare() {
        predictor.load(model)
    }

    override fun run(rawAudio: FloatArray, callback: (Result<LeakConfidence, Exception>) -> Unit) {
        val modelInput = preprocessor.run(rawAudio)

        predictor.run(modelInput) { modelOutput ->
            when (modelOutput) {
                is Err -> callback(modelOutput)
                is Ok -> {
                    val result = postprocessor.run(modelOutput.value)
                    callback(Ok(result))
                }
            }
        }
    }

}
