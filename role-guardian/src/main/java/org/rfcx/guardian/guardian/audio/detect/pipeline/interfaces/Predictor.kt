package org.rfcx.guardian.guardian.audio.detect.pipeline.interfaces

import android.content.Context

/**
 * An asyncronous pipeline step for performing prediction
 */
interface Predictor {
    val isLoaded: Boolean
    fun load(context: Context)
    fun run(input: FloatArray)
}
