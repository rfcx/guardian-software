package org.rfcx.guardian.guardian.view

import android.app.AlertDialog
import android.content.Context
import android.widget.NumberPicker
import androidx.constraintlayout.widget.ConstraintLayout
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.RfcxGuardian

class DurationPickerDialog(context: Context) : AlertDialog(context) {

    private lateinit var view: ConstraintLayout
    private lateinit var secondPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker

    private fun initDialog() {
        view = layoutInflater.inflate(R.layout.duration_picker_popup, null) as ConstraintLayout
        secondPicker = view.findViewById(R.id.secondPicker)
        minutePicker = view.findViewById(R.id.minutePicker)
    }

    private fun setPickerValues() {
        secondPicker.minValue = 0
        secondPicker.maxValue = 60
        minutePicker.minValue = 0
        minutePicker.maxValue = 60

        val app = context.applicationContext as RfcxGuardian
        val remainValue = app.rfcxPrefs.getPrefAsInt("audio_cycle_duration")
        secondPicker.value = remainValue % 60
        minutePicker.value = remainValue / 60
    }

    companion object {
        fun build(context: Context, onDurationSet: OnDurationSet): AlertDialog {
            val durationPickerDialog = DurationPickerDialog(context)
            durationPickerDialog.initDialog()
            durationPickerDialog.setPickerValues()
            return Builder(context)
                .setPositiveButton("Set") { _, _ ->
                    val totalSeconds = durationPickerDialog.secondPicker.value + (durationPickerDialog.minutePicker.value * 60)
                    onDurationSet.onSet(totalSeconds)
                }
                .setNegativeButton("Cancel", null)
                .setView(durationPickerDialog.view)
                .create()
        }
    }
}

interface OnDurationSet {
    fun onSet(seconds: Int)
}