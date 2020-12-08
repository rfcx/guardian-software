package org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces

/**
 * Pipeline step that performs transformation on an array of floats
 */
interface Preprocessor {
    fun run(input: FloatArray): FloatArray
}
