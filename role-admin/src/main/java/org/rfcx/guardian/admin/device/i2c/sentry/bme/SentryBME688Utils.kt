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

        val i2cConnectAttempt = app.deviceI2cUtils.i2cGetAsString("0x00", i2cMainAddr, true)
        val isI2cAccelChipConnected = abs(
            DeviceI2cUtils.twosComplementHexToDecAsLong(i2cConnectAttempt)
        ) > 0
        if (!isI2cAccelChipConnected) return false

        return true
    }

    fun getBME688Values(): BME688Att {
        System.out.format(
            "hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
            bme.humidityOversample, bme.temperatureOversample, bme.pressureOversample,
            bme.iirFilterConfig, bme.odr
        )
        bme.setConfiguration(
            BME68x.OversamplingMultiplier.X2, BME68x.OversamplingMultiplier.X2, BME68x.OversamplingMultiplier.X2,
            BME68x.IirFilterCoefficient._3, BME68x.ODR.NONE
        )
        System.out.format(
            "hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
            bme.humidityOversample, bme.temperatureOversample, bme.pressureOversample,
            bme.iirFilterConfig, bme.odr
        )
        Log.d("main", "set heater")
        val targetOperatingMode = BME68x.OperatingMode.FORCED
        bme.setHeaterConfiguration(targetOperatingMode, BME68x.HeaterConfig(true, 320, 150))

        Log.d("main", "getting duration")
        // Calculate delay period in microseconds
        val measureDurationMs =
            (bme.calculateMeasureDuration(targetOperatingMode) / 1000).toLong()
        // System.out.println("measure_duration_ms: " + measure_duration_ms + "
        // milliseconds");

        Log.d("main", "sleep")
        TimeUnit.MILLISECONDS.sleep(measureDurationMs)

        for (i in 0..4) {
            for ((reading, data) in bme.getSensorData(targetOperatingMode).withIndex()) {
                System.out.format(
                    "Reading [%d]: Idx: %,d. Temperature: %,.2f C. Pressure: %,.2f hPa. Relative Humidity: %,.2f %%rH. Gas Idx: %,d. Gas Resistance: %,.2f Ohms. IDAC: %,.2f mA. Gas Wait: %,d (ms or multiplier). (heater stable: %b, gas valid: %b).%n",
                    Integer.valueOf(reading),
                    Integer.valueOf(data.measureIndex),
                    java.lang.Float.valueOf(data.temperature),
                    java.lang.Float.valueOf(data.pressure),
                    java.lang.Float.valueOf(data.humidity),
                    Integer.valueOf(data.gasMeasurementIndex),
                    java.lang.Float.valueOf(data.gasResistance),
                    java.lang.Float.valueOf(data.idacHeatMA),
                    java.lang.Short.valueOf(data.gasWait),
                    java.lang.Boolean.valueOf(data.isHeaterTempStable),
                    java.lang.Boolean.valueOf(data.isGasMeasurementValid)
                )
            }

            TimeUnit.SECONDS.sleep(1)
        }
        return BME688Att(Date(),0.0,0.0,0.0,0.0)
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
