package org.rfcx.guardian.admin.comms.swm.control

import android.content.Context
import org.rfcx.guardian.admin.RfcxGuardian

class SwmPower(context: Context) {
    private val app = context.applicationContext as RfcxGuardian

    var on: Boolean
        get() = app.deviceGpioUtils.readGpioValue("satellite_power", "DOUT")
        set(value) {
            if (!value) {
                app.swmUtils.api.powerOff()
            }
            input(value)
        }

    init {
        // if power now is off and during working period then power swarm
        if (app.swmUtils.isSatelliteAllowedAtThisTimeOfDay()) {
            on = true
        }
    }

    private fun input(on: Boolean) {
        app.deviceGpioUtils.runGpioCommand("DOUT", "satellite_power", on)
    }
}
