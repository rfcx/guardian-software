package org.rfcx.guardian.guardian.utils

import android.content.Context
import org.rfcx.guardian.guardian.R

class AudioSettingUtils(context: Context) {

    //sample rate, bitrate display
    private val sampleRateDisplayList = context.resources.getStringArray(R.array.prefs_labels_audio_sample_rate)
    private val bitRateDisplayList = context.resources.getStringArray(R.array.prefs_labels_audio_encode_bitrate)

    //sample rate, bitrate data
    private val sampleRateList = context.resources.getStringArray(R.array.prefs_values_audio_sample_rate)
    private val bitRateList = context.resources.getStringArray(R.array.prefs_values_audio_encode_bitrate)

    fun getSampleRateLabel(sampleRate: String): String {
        return sampleRateDisplayList[sampleRateList.indexOf(sampleRate)]
    }

    fun getBitRateLabel(bitRate: String): String {
        return bitRateDisplayList[bitRateList.indexOf(bitRate)]
    }

}