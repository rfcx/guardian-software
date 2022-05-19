package org.rfcx.guardian.admin.device.i2c.sentry.infineon

import android.content.Context
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.i2c.DeviceI2cUtils
import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import kotlin.math.abs

class SentryInfineonUtils(context: Context) {

    private val app = context.applicationContext as RfcxGuardian

    private val infineon = Infineon(context)

    fun isChipAccessibleByI2c(): Boolean {
        val isNotExplicitlyDisabled: Boolean =
            app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_SENSOR_BME688)
        if (!isNotExplicitlyDisabled) return false

        val isI2cHandlerAccessible = app.deviceI2cUtils.isI2cHandlerAccessible
        if (!isI2cHandlerAccessible) return false

        val i2cConnectAttempt = app.deviceI2cUtils.i2cGetAsString("0x01", Infineon.MAIN_ADDRESS_REG, true, false)
        val isI2cAccelChipConnected = abs(
            DeviceI2cUtils.twosComplementHexToDecAsLong(i2cConnectAttempt)
        ) > 0
        if (!isI2cAccelChipConnected) return false

        return true
    }

}