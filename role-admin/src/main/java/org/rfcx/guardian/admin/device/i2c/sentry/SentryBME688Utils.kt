package org.rfcx.guardian.admin.device.i2c.sentry

import android.content.Context
import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.i2c.DeviceI2cUtils
import org.rfcx.guardian.utility.misc.ArrayUtils
import org.rfcx.guardian.utility.misc.DateTimeUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import kotlin.math.abs

class SentryBME688Utils(context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentryBME688Utils")

    private val app = context.applicationContext as RfcxGuardian
    private val i2cMainAddr = "0x68"

    private val bmeValues = arrayListOf<Double>()
    companion object {
        const val samplesTakenPerCaptureCycle = 1L
    }

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

    fun updateSentryAccelValues() {
        try {
            resetI2cTmpValues()
            for (i2cLabeledOutput in app.deviceI2cUtils.i2cGet(
                buildI2cQueryList(),
                SentryAccelUtils.i2cMainAddr,
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
                val valueSet: DoubleArray = this.i2cTmpValues.get(groupName)
                var valueTypeIndex = 0
                for (i in this.i2cValueIndex.indices) {
                    if (this.i2cValueIndex.get(i) == valueType) {
                        valueTypeIndex = i
                        break
                    }
                }
                valueSet[valueTypeIndex] =
                    if (i2cLabeledOutput[1] == null) 0 else SentryAccelUtils.applyValueModifier(
                        i2cLabeledOutput[0], i2cLabeledOutput[1]!!
                            .toLong()
                    )
                valueSet[4] = System.currentTimeMillis().toDouble()
                this.i2cTmpValues.put(groupName, valueSet)
            }
            cacheI2cTmpValues()
        } catch (e: Exception) {
            RfcxLog.logExc(SentryAccelUtils.logTag, e)
        }
    }

    fun saveBME688ValuesToDatabase(printValuesToLog: Boolean) {
        val sampleCount = this.accelValues.size
        if (sampleCount > 0) {
            val accVals = ArrayUtils.roundArrayValuesAndCastToLong(
                ArrayUtils.getAverageValuesAsArrayFromArrayList(this.accelValues)
            )
            this.accelValues = ArrayList<DoubleArray>()
            app.sentrySensorDb.dbAccelerometer.insert(
                accVals[4],
                accVals[0].toString() + "",
                accVals[1].toString() + "",
                accVals[2].toString() + "",
                accVals[3].toString() + ""
            )

            if (printValuesToLog) {
                Log.d(
                    logTag,
                    StringBuilder("Avg of ").append(sampleCount).append(" samples for ").append(
                        DateTimeUtils.getDateTime(
                            accVals[4]
                        )
                    ).toString()
                )
            }
        }
    }

    fun resetBMEValues() {
        bmeValues.clear()
    }
}
