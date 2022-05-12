package org.rfcx.guardian.admin.device.i2c.sentry.bme

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.i2c.DeviceI2cUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class SentryBME688Utils(context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentryBME688Utils")

    private val app = context.applicationContext as RfcxGuardian
    private val bme = BME68x(context)
    private val i2cMainAddr = "0x68"

    fun isChipAccessibleByI2c(): Boolean {
        val isNotExplicitlyDisabled: Boolean =
            app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_SENTRY_SENSOR)
        if (!isNotExplicitlyDisabled) return false

        val isI2cHandlerAccessible = app.deviceI2cUtils.isI2cHandlerAccessible
        if (isI2cHandlerAccessible) return false

        val i2cConnectAttempt = app.deviceI2cUtils.i2cGetAsString("0x00", i2cMainAddr, true, false)
        val isI2cAccelChipConnected = abs(
            DeviceI2cUtils.twosComplementHexToDecAsLong(i2cConnectAttempt)
        ) > 0
        if (!isI2cAccelChipConnected) return false

        return true
    }

    fun getBME688Values(): BME688Att {
        bme.setConfiguration(
            BME68x.OversamplingMultiplier.X2, BME68x.OversamplingMultiplier.X2, BME68x.OversamplingMultiplier.X2,
            BME68x.IirFilterCoefficient._3, BME68x.ODR.NONE
        )

        val targetOperatingMode = BME68x.OperatingMode.FORCED
        bme.setHeaterConfiguration(targetOperatingMode, BME68x.HeaterConfig(true, 320, 150))

        val measureDurationMs =
            (bme.calculateMeasureDuration(targetOperatingMode) / 1000).toLong()

        TimeUnit.MILLISECONDS.sleep(measureDurationMs)

        val bmeValues = BME688Att()
        for (i in 0..4) {
            for (data in bme.getSensorData(targetOperatingMode)) {
                if (i == 4) {
                    bmeValues.measuredAt = Date()
                    bmeValues.pressure = data.pressure
                    bmeValues.temperature = data.temperature
                    bmeValues.humidity = data.humidity
                    bmeValues.gas = data.gasResistance
                }
            }

            TimeUnit.SECONDS.sleep(1)
        }
        return bmeValues
    }

    fun saveBME688ValuesToDatabase(values: BME688Att) {
        app.sentrySensorDb.dbBME688.insert(
            values.measuredAt,
            values.pressure.toString(),
            values.humidity.toString(),
            values.temperature.toString(),
            values.gas.toString()
        )
    }

    fun getMomentaryConcatBME688ValuesAsJsonArray(): JSONArray {
        saveBME688ValuesToDatabase(getBME688Values())

        val bmeJsonArr = JSONArray()
        val bmeValues = app.sentrySensorDb.dbBME688.concatRows
        if (bmeValues != null) {
            val jsonObj = JSONObject()
            //TODO : filter values by prefs
            jsonObj.put("bme688", bmeValues)
            bmeJsonArr.put(jsonObj)
        }
        return bmeJsonArr
    }
}
