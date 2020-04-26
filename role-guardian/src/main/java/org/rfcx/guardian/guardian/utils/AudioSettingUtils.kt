package org.rfcx.guardian.guardian.utils

import android.content.Context
import org.rfcx.guardian.guardian.R

class AudioSettingUtils(context: Context) {

    //sample rate, bitrate display
    private val sampleRateDisplayList = context.resources.getStringArray(R.array.audio_sample_rate_labels)
    private val bitRateDisplayList = context.resources.getStringArray(R.array.audio_encode_bitrate_labels)

    //sample rate, bitrate data
    private val sampleRateList = context.resources.getStringArray(R.array.audio_sample_rate_values)
    private val bitRateList = context.resources.getStringArray(R.array.audio_encode_bitrate_values)

    fun getSampleRateLabel(sampleRate: String): String {
        return sampleRateDisplayList[sampleRateList.indexOf(sampleRate)]
    }

    fun getBitRateLabel(bitRate: String): String {
        return bitRateDisplayList[bitRateList.indexOf(bitRate)]
    }

}