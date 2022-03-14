package org.rfcx.guardian.admin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.device.android.control.ScheduledRebootService
import org.rfcx.guardian.utility.misc.DateTimeUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import java.util.*

class TimeChangedReceiver : BroadcastReceiver() {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "TimeChangedReceiver")

    override fun onReceive(context: Context, intent: Intent) {
        Log.v(logTag, "TimeChangedReceiver Launched...")

        // initializing rfcx application
        val app = context.applicationContext as RfcxGuardian
        app.initializedDateTime = Calendar.getInstance()

        // re-trigger services
        val service = arrayOf(ScheduledRebootService.SERVICE_NAME,
            DateTimeUtils.nextOccurrenceOf(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.REBOOT_FORCED_DAILY_AT)).timeInMillis.toString(),
            ScheduledRebootService.SCHEDULED_REBOOT_CYCLE_DURATION.toString())
        app.rfcxSvc.triggerService(service,true)
    }
}
