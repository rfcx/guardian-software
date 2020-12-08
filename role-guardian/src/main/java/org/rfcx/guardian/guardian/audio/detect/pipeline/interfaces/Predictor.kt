package org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces

import org.rfcx.guardian.guardian.audio.detect.pipeline.entities.Model
import org.rfcx.guardian.guardian.entity.Result

/**
 * An asyncronous pipeline step for performing prediction
 */
interface Predictor {
    val isLoaded: Boolean
    fun load(model: Model)
    fun run(input: FloatArray, callback: (Result<FloatArray, Exception>) -> Unit)
}
