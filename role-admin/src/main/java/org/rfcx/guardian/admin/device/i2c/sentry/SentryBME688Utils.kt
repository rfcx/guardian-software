package org.rfcx.guardian.admin.device.i2c.sentry

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.i2c.DeviceI2cUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import java.util.*
import kotlin.math.abs

class SentryBME688Utils(context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentryBME688Utils")

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

    fun getBME688Values(): BME688Att {
        for (i2cLabeledOutput in app.deviceI2cUtils.i2cGet(
            //TODO: to bme addresses
            listOf(listOf("").toTypedArray()),
            i2cMainAddr,
            true
        )) {
            val groupName = i2cLabeledOutput[0]!!.substring(
                0, i2cLabeledOutput[0]!!
                    .indexOf("-")
            )
            val valueType = i2cLabeledOutput[0]!!.substring(
                1 + i2cLabeledOutput[0]!!
                    .indexOf("-")
            )


//            valueSet[valueTypeIndex] =
//                if (i2cLabeledOutput[1] == null) 0 else SentryAccelUtils.applyValueModifier(
//                    i2cLabeledOutput[0], i2cLabeledOutput[1]!!
//                        .toLong()
//                )
//            valueSet[4] = System.currentTimeMillis().toDouble()
//            this.i2cTmpValues.put(groupName, valueSet)
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
