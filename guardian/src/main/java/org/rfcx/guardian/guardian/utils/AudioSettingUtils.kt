package org.rfcx.guardian.guardian.utils

object AudioSettingUtils {

    private val sampleRateDisplayList = arrayOf("8 kHz", "12 kHz", "16 kHz", "24 kHz", "44.1 kHz", "48 kHz")
    private val bitRateDisplayList = arrayOf(
        "4 kbps",
        "8 kbps",
        "12 kbps",
        "16 kbps",
        "20 kbps",
        "24 kbps",
        "32 kbps",
        "40 kbps",
        "48 kbps",
        "64 kbps",
        "80 kbps",
        "96 kbps",
        "112 kbps",
        "128 kbps"
    )

    private val sampleRateList = listOf(8000, 12000, 16000, 20000, 24000, 44100, 48000)
    private val bitRateList = listOf(4096, 8192, 12288, 16384, 20480, 24576, 32768, 40960, 49152, 65536, 81920, 98304, 114688, 131072)

    fun getSampleRateLabel(sampleRate: Int): String {
        return sampleRateDisplayList[sampleRateList.indexOf(sampleRate)]
    }

    fun getBitRateLabel(bitRate: Int): String {
        return bitRateDisplayList[bitRateList.indexOf(bitRate)]
    }

}