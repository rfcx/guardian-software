package org.rfcx.guardian.classify.model.interfaces

/**
 * An asyncronous pipeline step for performing prediction
 */
interface Predictor {
    val isLoaded: Boolean
    fun load()
    fun run(input: FloatArray): FloatArray
}
