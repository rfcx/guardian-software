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
import org.rfcx.guardian.admin.comms.swm.data.SwmRTBackgroundResponse
import org.rfcx.guardian.admin.comms.swm.data.SwmRTResponse
import org.rfcx.guardian.admin.comms.swm.data.SwmUnsentMsg

import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import kotlin.math.log

class SwmUtils(private val context: Context) {
    var app: RfcxGuardian = context.applicationContext as RfcxGuardian
    lateinit var power: SwmPower
    lateinit var api: SwmApi

    private var swmId: String? = null

    fun setupSwmUtils() {
        power = SwmPower(context)
        api = SwmApi(SwmConnection(SwmUartShell()))
    }

    fun isSatelliteAllowedAtThisTimeOfDay(): Boolean {
        for (offHoursRange in app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_OFF_HOURS)
            .split(",")) {
            val offHours = offHoursRange.split("-")
            if (DateTimeUtils.isTimeStampWithinTimeRange(Date(), offHours[0], offHours[1])) {
                return false
            }
        }
        return true
    }

    fun getMomentaryConcatDiagnosticValuesAsJsonArray(): JSONArray {
        saveBackgroundSignal()
        val swmDiagnosticJSONarr = JSONArray()
        val rssi = app.swmMetaDb.dbSwmDiagnostic.concatRowsIgnoreNull
        if (rssi.isNotEmpty()) {
            val diagnosticJson = JSONObject()
            diagnosticJson.put("swm", rssi)
            swmDiagnosticJSONarr.put(diagnosticJson)
        }
        return swmDiagnosticJSONarr
    }

    fun saveDiagnostic() {
        val rtBackground = api.getRTBackground()
        val rtSatellite = api.getRTSatellite()
        val unsentMessageNumbers = api.getNumberOfUnsentMessages()

        var rssiBackground: Int? = null
        if (rtBackground != null) {
            rssiBackground = rtBackground.rssi
        }

        var rssiSat: Int? = null
        var snr: Int? = null
        var fdev: Int? = null
        var time: String? = null
        var satId: String? = null
        if (rtSatellite != null) {
            Log.d(logTag, "Saving Satellite Packet")
            if (rtSatellite.packetTimestamp != "1970-01-01 00:00:00") time = rtSatellite.packetTimestamp
            if (time != null) {
                if (rtSatellite.rssi != 0) rssiSat = rtSatellite.rssi
                if (rtSatellite.signalToNoiseRatio != 0) snr = rtSatellite.signalToNoiseRatio
                if (rtSatellite.frequencyDeviation != 0) fdev = rtSatellite.frequencyDeviation
                if (rtSatellite.satelliteId != "0x000000") satId = rtSatellite.satelliteId
            }
        }

        if (rtBackground != null || rtSatellite != null) {
            app.swmMetaDb.dbSwmDiagnostic.insert(
                rssiBackground,
                rssiSat,
                snr,
                fdev,
                time,
                satId,
                unsentMessageNumbers
            )
        }
    }

    private fun saveBackgroundSignal() {
        if (::api.isInitialized) {
            val rtBackground = api.getRTBackground()
            var rssiBackground: Int? = null
            if (rtBackground != null) {
                rssiBackground = rtBackground.rssi
            }

            if (rtBackground != null) {
                app.swmMetaDb.dbSwmDiagnostic.insert(
                    rssiBackground,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            }
        }
    }

    fun getSwmId(): String? {
        if (swmId != null || !::api.isInitialized) return swmId
        swmId = api.getSwarmDeviceId()
        return swmId
    }

    companion object {
        private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmUtils")

        // Scheduling Tools
        @kotlin.jvm.JvmStatic
        fun addScheduledSwmToQueue(
            sendAtOrAfter: Long,
            groupId: String?,
            msgPayload: String?,
            priority: Int,
            context: Context,
            triggerDispatchService: Boolean
        ): Boolean {
            val isQueued = false
            if (msgPayload != null && !msgPayload.equals("", ignoreCase = true)) {
                val app = context.applicationContext as RfcxGuardian
                val msgId = DeviceSmsUtils.generateMessageId()
                app.swmMessageDb.dbSwmQueued.insert(sendAtOrAfter, "", msgPayload, groupId, msgId, priority)
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
