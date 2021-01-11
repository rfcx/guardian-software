package org.rfcx.guardian.classify.utils

import android.content.Context
import org.rfcx.guardian.classify.RfcxGuardian
import org.rfcx.guardian.classify.model.AudioClassifier
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.io.File

class AudioClassifyModelUtils(context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyModelUtils")

    private var classifier: AudioClassifier? = null

    fun classifyAudio(file: File) {
        val path = file.absolutePath
        classifyAudio(path)
    }

    fun classifyAudio(path: String): List<FloatArray> {
        return this.classifier?.classify(path) ?: listOf()
    }

}
