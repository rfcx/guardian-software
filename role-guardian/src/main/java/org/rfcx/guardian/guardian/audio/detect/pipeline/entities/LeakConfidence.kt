package org.rfcx.guardian.guardian.audio.detect.pipeline.entities

/**
 * TODO: Add a class description
 */

data class LeakConfidence(val value: Float) {
    companion object {
        const val minLeakValue: Float = 0.75f
        const val maxNormalValue: Float = 0.25f
    }
    val formattedPercentage: String get() = "%.1f %%".format(value*100)
}
