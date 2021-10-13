package org.rfcx.guardian.admin.comms.swm.control

import android.content.Context
import org.rfcx.guardian.admin.RfcxGuardian

class SwmPower(context: Context) {
    private val app = context.applicationContext as RfcxGuardian

    init {
        // if power now is off and during working period then power swarm
        if (app.swmUtils.isSatelliteAllowedAtThisTimeOfDay()) {
            powerOnModem()
        }
    }

    fun powerOffModem() {
        if (on) {
            // run shutdown UART command
            app.swmUtils.api.powerOff()
            // after return, kill power to tile
            on = false
        }
    }

    fun powerOnModem() {
        if (!on) {
            on = true
        }
    }

    var on: Boolean
        get() = app.deviceGpioUtils.readGpioValue("satellite_power", "DOUT")
        set(value) { app.deviceGpioUtils.runGpioCommand("DOUT", "satellite_power", value) }
}
