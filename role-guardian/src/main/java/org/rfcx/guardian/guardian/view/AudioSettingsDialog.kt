package org.rfcx.guardian.guardian.view

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxPrefs

data class AudioSettings(
    val sampleRate: String,
    val bitRate: String,
    val fileFormat: String
)

class AudioSettingsDialog(context: Context) : AlertDialog(context) {
    //sample rate, bitrate display
    private val sampleRateDisplayList =
        context.resources.getStringArray(R.array.prefs_labels_audio_sample_rate)
    private val bitRateDisplayList =
        context.resources.getStringArray(R.array.prefs_labels_audio_encode_bitrate)

    //sample rate, bitrate data
    private val sampleRateList =
        context.resources.getStringArray(R.array.prefs_values_audio_sample_rate)
    private val bitRateList =
        context.resources.getStringArray(R.array.prefs_values_audio_encode_bitrate)

    private var fileFormat = ""

    private lateinit var view: ConstraintLayout
    private lateinit var opusRadio: RadioButton
    private lateinit var flacRadio: RadioButton
    private lateinit var sampleRatePicker: NumberPicker
    private lateinit var bitRatePicker: NumberPicker
    private lateinit var bitRateLabel: TextView

    private lateinit var app: RfcxGuardian

    private fun initDialog() {
        view =
            layoutInflater.inflate(R.layout.audio_settings_picker_popup, null) as ConstraintLayout
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

        app = context.applicationContext as RfcxGuardian
        val remainFileFormat = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_STREAM_CODEC)
        fileFormat = remainFileFormat
        val remainSampleRate = app.audioCaptureUtils.requiredCaptureSampleRate
        val remainBitRate = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_STREAM_BITRATE)

        if (remainFileFormat == "opus") {
            opusRadio.isChecked = true
            bitRatePicker.visibility = View.VISIBLE
            bitRateLabel.visibility = View.VISIBLE
        } else {
            flacRadio.isChecked = true
            bitRatePicker.visibility = View.GONE
            bitRateLabel.visibility = View.GONE
        }

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
                bitRatePicker.value =
                    bitRateList.indexOf(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_STREAM_BITRATE))
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