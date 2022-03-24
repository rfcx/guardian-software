package org.rfcx.guardian.admin.comms.swm.control

import android.content.Context
import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.misc.DateTimeUtils
import org.rfcx.guardian.utility.misc.TimeUtils.isNowOutsideTimeRange
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs

class SwmPower(context: Context) {
    private val app = context.applicationContext as RfcxGuardian
    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmPower")

    var on: Boolean
        get() = getStatus()
        set(value) {
            if (value == getStatus()) return
            if (!value) {
                Log.d(logTag, "POWER OFF MODEM")
                app.swmUtils.api.powerOff()
            } else {
                Log.d(logTag, "POWER ON MODEM")
            }
            setStatus(value)
        }

    init {
        // if power now is off and during working period then power swarm
        if (isNowOutsideTimeRange(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_OFF_HOURS)) || DateTimeUtils.isCurrentTimeBefore2022()) {
            on = true
        }
    }

    private fun getStatus(): Boolean {
        return app.deviceGpioUtils.readGpioValue("satellite_power", "DOUT")
    }

    private fun setStatus(on: Boolean) {
        app.deviceGpioUtils.runGpioCommand("DOUT", "satellite_power", on)
    }
}
