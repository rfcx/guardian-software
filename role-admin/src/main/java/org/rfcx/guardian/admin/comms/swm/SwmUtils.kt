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

class SwmUtils(private val context: Context) {
    var app: RfcxGuardian = context.applicationContext as RfcxGuardian
    lateinit var power: SwmPower
    lateinit var api: SwmApi

    @kotlin.jvm.JvmField
    var isInFlight = false

    @kotlin.jvm.JvmField
    var consecutiveDeliveryFailureCount = 0

    fun setupSwmUtils() {
        power = SwmPower(context)
        api = SwmApi(SwmConnection(SwmUartShell()))
        app.rfcxSvc.triggerService(SwmDiagnosticService.SERVICE_NAME, true)
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

    fun updateQueueMessagesFromSwarm(swmUnsentMessages: List<SwmUnsentMsg>?) {
        val swarmMessageIdQueues = swmUnsentMessages?.map { it.messageId } ?: return
        val guardianMessageIdQueues = app.swmMessageDb.dbSwmQueued.allRows.filter { it.getOrNull(5) != null }
        Log.d(logTag, "Swarm queue: ${swarmMessageIdQueues.size}  Guardian queue: ${guardianMessageIdQueues.size}")
        for (guardianMessage in guardianMessageIdQueues) {
            if (!swarmMessageIdQueues.contains(guardianMessage[5])) {
                app.swmMessageDb.dbSwmSent.insert(
                    guardianMessage[1].toLong(),
                    guardianMessage[2],
                    guardianMessage[3],
                    guardianMessage[4],
                    guardianMessage[5]
                )
                app.swmMessageDb.dbSwmQueued.deleteSingleRowBySwmMessageId(guardianMessage[5])
            }
        }
    }

    companion object {
        private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmUtils")
        const val sendCmdTimeout: Long = 70000
        const val prepCmdTimeout: Long = 2500
        const val powerCycleAfterThisManyConsecutiveDeliveryFailures = 5

        // Scheduling Tools
        @kotlin.jvm.JvmStatic
        fun addScheduledSwmToQueue(
            sendAtOrAfter: Long,
            msgPayload: String?,
            context: Context,
            triggerDispatchService: Boolean
        ): Boolean {
            val isQueued = false
            if (msgPayload != null && !msgPayload.equals("", ignoreCase = true)) {
                val app = context.applicationContext as RfcxGuardian
                val msgId = DeviceSmsUtils.generateMessageId()
                app.swmMessageDb.dbSwmQueued.insert(sendAtOrAfter, "", msgPayload, msgId)
                if (triggerDispatchService) {
                    app.rfcxSvc.triggerService(SwmDispatchService.SERVICE_NAME, false)
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

        @kotlin.jvm.JvmStatic
        fun findRunningSerialProcessIds(busyBoxBin: String): IntArray {
            val processIds: MutableList<Int> = ArrayList()
            if (!FileUtils.exists(busyBoxBin)) {
                Log.e(
                    logTag,
                    "Could not run findRunningSerialProcessIds(). BusyBox binary not found on system."
                )
            } else {
                val processScan =
                    ShellCommands.executeCommandAsRoot("$busyBoxBin ps -ef | grep /dev/ttyMT")
                for (scanRtrn in processScan) {
                    if (scanRtrn.contains("microcom") || scanRtrn.contains("stty")) {
                        val processId = scanRtrn.substring(0, scanRtrn.indexOf("root"))
                        processIds.add(processId.toInt())
                    }
                }
            }
            return ArrayUtils.ListToIntArray(processIds)
        }
    }
}
