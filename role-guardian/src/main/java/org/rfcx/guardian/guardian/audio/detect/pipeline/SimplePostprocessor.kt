package org.rfcx.guardian.guardian.audio.detect.pipeline

import org.rfcx.guardian.guardian.audio.detect.pipeline.entities.LeakConfidence
import org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces.Postprocessor

/**
 * A postprocessor for multi-class models where only 1 output is used
 */
class SimplePostprocessor(private val selectedIndex: Int): Postprocessor {
    override fun run(input: FloatArray): LeakConfidence {
        return LeakConfidence(input[selectedIndex])
    }
}
