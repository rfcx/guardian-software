package org.rfcx.guardian.guardian.audio.detect.pipeline.entities

/**
 * An AI model
 */

open class Model() {
    // Local only
    var filePath: String = ""

    val originalFilename: String
        get() = filePath.split('/').last().dropLast(7) // .tflite

    companion object {
        // Note: in future, these could be downloaded from the API
        val inputShape: List<Int> = listOf(1, 1024)
        val outputShape: List<Int> = listOf(1, 10)
        val outputIndexOfLeak: Int = 1
    }
}
