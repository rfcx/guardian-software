package org.rfcx.guardian.guardian.view

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.audio_settings_picker_popup.*
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.RfcxGuardian

data class AudioSettings(
    val sampleRate: Int,
    val bitRate: Int,
    val fileFormat: String
)

class AudioSettingsDialog(context: Context) : AlertDialog(context) {
    //sample rate, bitrate display
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

    //sample rate, bitrate data
    private val sampleRateList = listOf(8000, 12000, 16000, 20000, 24000, 44100, 48000)
    private val bitRateList = listOf(4096, 8192, 12288, 16384, 20480, 24576, 32768, 40960, 49152, 65536, 81920, 98304, 114688, 131072)

    private var fileFormat = ""

    private lateinit var view: ConstraintLayout
    private lateinit var opusRadio: RadioButton
    private lateinit var flacRadio: RadioButton
    private lateinit var sampleRatePicker: NumberPicker
    private lateinit var bitRatePicker: NumberPicker
    private lateinit var bitRateLabel: TextView

    private fun initDialog() {
        view = layoutInflater.inflate(R.layout.audio_settings_picker_popup, null) as ConstraintLayout
        sampleRatePicker = view.findViewById(R.id.sampleRatePicker)
        bitRatePicker = view.findViewById(R.id.bitRatePicker)
        opusRadio = view.findViewById(R.id.opusRb)
        flacRadio = view.findViewById(R.id.flacRb)
        bitRateLabel = view.findViewById(R.id.bitRateLabel)
    }

    private fun setPickerValues() {
        sampleRatePicker.minValue = 0
        sampleRatePicker.maxValue = sampleRateDisplayList.size - 1
        sampleRatePicker.displayedValues = sampleRateDisplayList
        bitRatePicker.minValue = 0
        bitRatePicker.maxValue = bitRateDisplayList.size - 1
        bitRatePicker.displayedValues = bitRateDisplayList

        val app = context.applicationContext as RfcxGuardian
        val remainFileFormat = app.rfcxPrefs.getPrefAsString("audio_encode_codec")
        fileFormat = remainFileFormat
        val remainSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate")
        val remainBitRate = app.rfcxPrefs.getPrefAsInt("audio_encode_bitrate")

        if (remainFileFormat == "opus") {
            opusRadio.isChecked = true
            bitRatePicker.visibility = View.VISIBLE
            bitRateLabel.visibility = View.VISIBLE
        } else {
            flacRadio.isChecked = true
            bitRatePicker.visibility = View.GONE
            bitRateLabel.visibility = View.GONE
        }

        sampleRatePicker.value = sampleRateList.indexOf(remainSampleRate)
        bitRatePicker.value = bitRateList.indexOf(remainBitRate)

        setRadioActionListener()
    }

    private fun setRadioActionListener() {
        opusRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bitRatePicker.visibility = View.VISIBLE
                bitRateLabel.visibility = View.VISIBLE
                fileFormat = "opus"
            } else {
                bitRatePicker.visibility = View.GONE
                bitRateLabel.visibility = View.GONE
                fileFormat = "flac"
            }
        }
    }

    companion object {
        fun build(context: Context, onAudioSettingsSet: OnAudioSettingsSet): AlertDialog {
            val audioSettingsDialog = AudioSettingsDialog(context)
            audioSettingsDialog.initDialog()
            audioSettingsDialog.setPickerValues()
            return Builder(context)
                .setPositiveButton("Set") { _, _ ->
                    val audioSettings = AudioSettings(
                        sampleRate = audioSettingsDialog.sampleRateList[audioSettingsDialog.sampleRatePicker.value],
                        bitRate = audioSettingsDialog.bitRateList[audioSettingsDialog.bitRatePicker.value],
                        fileFormat = audioSettingsDialog.fileFormat
                    )
                    onAudioSettingsSet.onSet(audioSettings)
                }
                .setNegativeButton("Cancel", null)
                .setView(audioSettingsDialog.view)
                .create()
        }
    }
}

interface OnAudioSettingsSet {
    fun onSet(settings: AudioSettings)
}