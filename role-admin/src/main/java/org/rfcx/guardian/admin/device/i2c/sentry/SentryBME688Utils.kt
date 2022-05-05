package org.rfcx.guardian.admin.device.i2c.sentry

import android.content.Context
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.i2c.DeviceI2cUtils
import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import kotlin.math.abs

class SentryBME688Utils(context: Context) {

    private val app = context.applicationContext as RfcxGuardian
    private val i2cMainAddr = "0x68"

    fun isChipAccessibleByI2c(): Boolean {
        val isNotExplicitlyDisabled: Boolean =
            app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_SENTRY_SENSOR)
        if (!isNotExplicitlyDisabled) return false

        val isI2cHandlerAccessible = app.deviceI2cUtils.isI2cHandlerAccessible
        if (isI2cHandlerAccessible) return false

        val i2cConnectAttempt = app.deviceI2cUtils.i2cGetAsString("0x00", i2cMainAddr, true)
        val isI2cAccelChipConnected = abs(
            DeviceI2cUtils.twosComplementHexToDecAsLong(i2cConnectAttempt)
        ) > 0
        if (!isI2cAccelChipConnected) return false

        return true
    }
}
