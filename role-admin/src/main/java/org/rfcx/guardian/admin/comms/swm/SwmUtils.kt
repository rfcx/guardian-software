package org.rfcx.guardian.admin.comms.swm

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import org.rfcx.guardian.admin.comms.swm.api.SwmUartShell
import org.rfcx.guardian.utility.device.DeviceSmsUtils
import org.rfcx.guardian.utility.misc.ArrayUtils
import org.rfcx.guardian.utility.misc.FileUtils
import org.rfcx.guardian.utility.misc.ShellCommands
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.util.*
import org.rfcx.guardian.utility.misc.DateTimeUtils

import android.text.TextUtils
import org.rfcx.guardian.admin.comms.swm.control.SwmPower
import org.rfcx.guardian.admin.comms.swm.data.SwmUnsentMsg

import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import kotlin.math.log

class SwmUtils(private val context: Context) {
    var app: RfcxGuardian = context.applicationContext as RfcxGuardian
    lateinit var power: SwmPower
    lateinit var api: SwmApi

    fun setupSwmUtils() {
        Log.d(logTag, "X")
        power = SwmPower(context)
        Log.d(logTag, "Y")
        api = SwmApi(SwmConnection(SwmUartShell()))
        Log.d(logTag, "Z")
    }

    fun isSatelliteAllowedAtThisTimeOfDay(): Boolean {
        for (offHoursRange in app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_OFF_HOURS).split(",")) {
            val offHours = offHoursRange.split("-")
            if (DateTimeUtils.isTimeStampWithinTimeRange(Date(), offHours[0], offHours[1])) {
                return false
            }
        }
        return true
    }

    fun isLastSatellitePacketAllowToSave(lastDate: String): Boolean {
        val minRange = 1000 * 60 * 3
        val lastTime = DateTimeUtils.getDateFromStringUTC(lastDate).time
        val currentTime = DateTimeUtils.getCurrentTimeInUTC()
        Log.d(logTag, "$currentTime $lastTime")
        if (currentTime - lastTime > minRange) return false
        return true
    }

    companion object {
        private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmUtils")

        // Scheduling Tools
        @kotlin.jvm.JvmStatic
        fun addScheduledSwmToQueue(
            sendAtOrAfter: Long,
            groupId: String?,
            msgPayload: String?,
            context: Context,
            triggerDispatchService: Boolean
        ): Boolean {
            val isQueued = false
            if (msgPayload != null && !msgPayload.equals("", ignoreCase = true)) {
                val app = context.applicationContext as RfcxGuardian
                val msgId = DeviceSmsUtils.generateMessageId()
                app.swmMessageDb.dbSwmQueued.insert(sendAtOrAfter, "", msgPayload, groupId, msgId)
                if (triggerDispatchService) {
                    app.rfcxSvc.triggerService(SwmDispatchCycleService.SERVICE_NAME, false)
                }
            }
            return isQueued
        }

        @kotlin.jvm.JvmStatic
        fun getSwmMetaValuesAsJsonArray(context: Context): JSONArray {
            val app = context.applicationContext as RfcxGuardian
            val metaJsonArray = JSONArray()
            try {
                val metaJson = JSONObject()
                metaJson.put("swm", app.swmMetaDb.dbSwmDiagnostic.concatRowsIgnoreNull)
                metaJsonArray.put(metaJson)
            } catch (e: Exception) {
                RfcxLog.logExc(logTag, e)
            }
            return metaJsonArray
        }

        @kotlin.jvm.JvmStatic
        fun deleteSwmMetaValuesBeforeTimestamp(timeStamp: String, context: Context): Int {
            val app = context.applicationContext as RfcxGuardian
            val clearBefore = Date(timeStamp.toLong())
            app.swmMetaDb.dbSwmDiagnostic.clearRowsBefore(clearBefore)
            return 1
        }
    }
}
