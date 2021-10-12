package org.rfcx.guardian.admin.comms.swm.control

import android.content.Context
import android.text.TextUtils
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.misc.DateTimeUtils
import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import java.util.*

class SwmPower(context: Context) {
    private val app = context.applicationContext as RfcxGuardian

    init {
        // if power now is off and during working period then power swarm
        if (!isPowerOn && isSatelliteAllowedAtThisTimeOfDay()) {
            setPower(true)
        }
    }

    fun powerOffModem() {
        if (isPowerOn) {
            // run shutdown UART command
            app.swmUtils.api.powerOff()
            // after return, kill power to tile
            setPower(false)
        }
    }

    fun setPower(setToOn: Boolean) {
        app.deviceGpioUtils.runGpioCommand("DOUT", "satellite_power", setToOn)
    }

    val isPowerOn: Boolean
        get() = app.deviceGpioUtils.readGpioValue("satellite_power", "DOUT")

    fun isSatelliteAllowedAtThisTimeOfDay(): Boolean {
        for (offHoursRange in TextUtils.split(
            app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_OFF_HOURS),
            ","
        )) {
            val offHours = TextUtils.split(offHoursRange, "-")
            if (DateTimeUtils.isTimeStampWithinTimeRange(Date(), offHours[0], offHours[1])) {
                return false
            }
        }
        return true
    }
}
