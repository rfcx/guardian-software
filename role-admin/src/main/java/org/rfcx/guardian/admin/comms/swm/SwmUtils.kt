package org.rfcx.guardian.admin.comms.swm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import org.rfcx.guardian.admin.comms.swm.api.SwmUartShell
import org.rfcx.guardian.admin.comms.swm.control.SwmPower
import org.rfcx.guardian.utility.device.DeviceSmsUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.util.*

class SwmUtils(private val context: Context) {
    var app: RfcxGuardian = context.applicationContext as RfcxGuardian
    lateinit var power: SwmPower
    lateinit var api: SwmApi

    private var swmId: String? = null
    private var isGPSConnected: Boolean? = null
    var sleepFlag = false

    fun setupSwmUtils() {
        power = SwmPower(context)
        api = SwmApi(SwmConnection(SwmUartShell()))
    }

    fun getMomentaryConcatDiagnosticValuesAsJsonArray(): JSONArray {
        val diagnostic = getDiagnostic()
        val swmDiagnosticJSONarr = JSONArray()
        if (diagnostic.isNotEmpty()) {
            val diagnosticJson = JSONObject()
            diagnosticJson.put("swm", diagnostic)
            swmDiagnosticJSONarr.put(diagnosticJson)
        }
        return swmDiagnosticJSONarr
    }

    fun getDiagnostic(): String {
        val rtBackground = api.getRTBackground()
        val rtSatellite = api.getRTSatellite()
        val unsentMessageNumbers = api.getNumberOfUnsentMessages()
        sleepFlag = false
        var swmBlob = ""

        if (rtBackground != null) {
            // Same style as query from db
            swmBlob += "${Date().time}*${rtBackground.rssi}"
            if (rtSatellite != null && rtSatellite.packetTimestamp != "1970-01-01 00:00:00") {
                swmBlob += "*${rtSatellite.rssi}*${rtSatellite.signalToNoiseRatio}*${rtSatellite.frequencyDeviation}*${rtSatellite.packetTimestamp}*${rtSatellite.satelliteId}"
            }
            swmBlob += "*$unsentMessageNumbers"
        }
        return swmBlob
    }

    fun getSwmId(): String? {
        if (swmId != null || !::api.isInitialized) return swmId
        swmId = api.getSwarmDeviceId()
        sleepFlag = false
        return swmId
    }

    fun getGPSConnection(): Boolean? {
        if (isGPSConnected != null || !::api.isInitialized) return isGPSConnected
        api.getGPSConnection() ?: return null
        sleepFlag = false
        isGPSConnected = true
        return isGPSConnected
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
                app.swmMessageDb.dbSwmQueued.insert(
                    sendAtOrAfter,
                    "",
                    msgPayload,
                    groupId,
                    msgId,
                    priority
                )
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
                metaJson.put("swm", app.swmUtils.getDiagnostic())
                metaJsonArray.put(metaJson)
            } catch (e: Exception) {
                RfcxLog.logExc(logTag, e)
            }
            return metaJsonArray
        }
    }
}
