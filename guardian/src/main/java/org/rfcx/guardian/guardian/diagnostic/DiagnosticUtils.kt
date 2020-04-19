package org.rfcx.guardian.guardian.diagnostic

import android.content.Context
import org.rfcx.guardian.guardian.RfcxGuardian

class DiagnosticUtils(context: Context) {

    private val app = context as RfcxGuardian

    fun updateRecordedDiagnostic() {
        val recordedList = app.diagnosticDb.dbRecordedDiagnostic.latestRow
        val recordCycle: Int = app.rfcxPrefs.getPrefAsInt("audio_cycle_duration")
        val recorded = recordedList[1].toInt() + 1
        val amount = recordedList[2].toInt() + recordCycle
        app.diagnosticDb.dbRecordedDiagnostic.updateRecordedAndTime(recorded, amount)
    }

    fun updateSyncedDiagnostic() {
        val syncedList = app.diagnosticDb.dbSyncedDiagnostic.latestRow
        val recordCycle = app.rfcxPrefs.getPrefAsInt("audio_cycle_duration")
        val synced = syncedList[1].toInt() + 1
        val amount = syncedList[2].toInt() + recordCycle
        app.diagnosticDb.dbSyncedDiagnostic.updateSyncedAndTime(synced, amount)
    }

    fun secondToTime(second: Int?): String {
        if (second != null) {
            var amountTime = ""
            val minutes = second / 60
            amountTime = if (minutes > 60L) {
                val hours = minutes / 60
                val min = minutes % 60
                if (min == 0) {
                    "$hours hours"
                } else {
                    "$hours hours and $min minutes"
                }
            } else {
                "$minutes minutes"
            }

            return amountTime
        } else {
            return "none"
        }
    }
}
