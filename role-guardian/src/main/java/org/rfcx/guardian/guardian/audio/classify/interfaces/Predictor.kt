package org.rfcx.guardian.guardian.audio.classify.interfaces

import android.content.Context

/**
 * An asyncronous pipeline step for performing prediction
 */
interface Predictor {
    val isLoaded: Boolean
    fun load()
    fun run(input: FloatArray): FloatArray
}
