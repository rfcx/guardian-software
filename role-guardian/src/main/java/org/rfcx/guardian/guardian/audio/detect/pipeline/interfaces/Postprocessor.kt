package org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces

import org.rfcx.guardian.guardian.audio.detect.pipeline.entities.LeakConfidence

/**
 * Pipeline step to convert predictor outputs to a LeakConfidence
 */
interface Postprocessor {
    fun run(input: FloatArray): LeakConfidence
}
