package org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces

import org.rfcx.guardian.guardian.audio.detect.pipeline.entities.LeakConfidence
import org.rfcx.guardian.guardian.entity.Result

/**
 * A Pipeline takes a waveform as an array of floats, and outputs a LeakConfidence
 */

interface Pipeline {
    val isReady: Boolean

    fun prepare()
    fun run(rawAudio: FloatArray, callback: (Result<LeakConfidence,Exception>) -> Unit)
}
