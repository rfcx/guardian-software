package org.rfcx.guardian.admin.comms.swm

import android.content.Context
import android.text.TextUtils
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.device.DeviceSmsUtils
import org.rfcx.guardian.utility.misc.ArrayUtils
import org.rfcx.guardian.utility.misc.DateTimeUtils
import org.rfcx.guardian.utility.misc.FileUtils
import org.rfcx.guardian.utility.misc.ShellCommands
import org.rfcx.guardian.utility.rfcx.RfcxComm
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.util.*

class SwmUtils(context: Context) {
    var app: RfcxGuardian
    private var busyBoxBin: String? = null
    private var ttyPath: String? = null
    @kotlin.jvm.JvmField
	var isInFlight = false
    @kotlin.jvm.JvmField
	var consecutiveDeliveryFailureCount = 0
    fun init(ttyPath: String?, busyBoxBin: String?) {
        this.ttyPath = ttyPath
        this.busyBoxBin = busyBoxBin
    }

    fun setupSwmUtils() {
        app.deviceGpioUtils.runGpioCommand("DOUT", "voltage_refr", true)
        setPower(true)
        setPower(false)
    }

    fun findRunningSerialProcessIds(): IntArray {
        val processIds: MutableList<Int> = ArrayList()
        isInFlight = false
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

    fun setPower(setToOn: Boolean) {
        app.deviceGpioUtils.runGpioCommand("DOUT", "satellite_power", setToOn)
    }

    val isPowerOn: Boolean
        get() = app.deviceGpioUtils.readGpioValue("satellite_power", "DOUT")
    val isNetworkAvailable: Boolean
        get() = app.deviceGpioUtils.readGpioValue("satellite_state", "DOUT")

    fun sendSwmMessage(msgStr: String): Boolean {
        var errorMsg = "SWM Message was NOT successfully delivered."
        try {
            if (!FileUtils.exists(busyBoxBin)) {
                errorMsg = "BusyBox binary not found on system"
            } else {
                val command = "TD $msgStr"
                val atCmdSeq = arrayOf("$" + command + "*" + SwmCommandChecksum.get(command))
                Log.d(
                    logTag,
                    DateTimeUtils.getDateTime() + " - Attempting TD Command Sequence: " + TextUtils.join(
                        ", ",
                        atCmdSeq
                    )
                )
                app.rfcxSvc.triggerService(SwmDispatchService.SERVICE_NAME, true)
                val atCmdResponseLines =
                    ShellCommands.executeCommandAsRoot(atCmdExecStr(ttyPath, busyBoxBin, atCmdSeq))
                for (atCmdResponseLine in atCmdResponseLines) {
                    errorMsg += if (atCmdResponseLine!!.contains("OK")) {
                        Log.i(
                            logTag,
                            DateTimeUtils.getDateTime() + " - SWM Sending Message was successfully transmitted "
                        )
                        return true
                    } else {
                        " TD Response: " + TextUtils.join(", ", atCmdResponseLines)
                    }
                }
            }
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        }
        Log.e(logTag, errorMsg)
        return false
    }

    private fun updateQueueMessagesFromSwarm(): Boolean {
        val errorMsg = "SWM Sleep Command was NOT successfully delivered."
        try {
            val guardianMessageIdQueues = app.swmMessageDb.dbSwmQueued.allRows
            val swarmMessageIdQueues = ArrayList<String>()
            val command = "MT L=U"
            val atCmdSeq = arrayOf("$" + command + "*" + SwmCommandChecksum.get(command))
            Log.d(
                logTag,
                DateTimeUtils.getDateTime() + " - Attempting Query Unsent Message Command : " + TextUtils.join(
                    ", ",
                    atCmdSeq
                )
            )
            val atCmdResponseLines =
                ShellCommands.executeCommandAsRoot(atCmdExecStr(ttyPath, busyBoxBin, atCmdSeq))
            for (atCmdResponseLine in atCmdResponseLines) {
                //                            hexdecimal data         message id   timestamp
                // Example message : $MT 68692066726f6d20737761726d,4428826476689,1605639598*55
                swarmMessageIdQueues.add(atCmdResponseLine.split(",").toTypedArray()[1])
            }
            for (guardianMessage in guardianMessageIdQueues) {
                if (!swarmMessageIdQueues.contains(guardianMessage[4])) {
                    app.swmMessageDb.dbSwmSent.insert(
                        guardianMessage[1].toLong(),
                        guardianMessage[2],
                        guardianMessage[3],
                        guardianMessage[4]
                    )
                    app.swmMessageDb.dbSwmQueued.deleteSingleRowByMessageId(guardianMessage[4])
                }
            }
            return true
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        }
        Log.e(logTag, errorMsg)
        return false
    }

    private fun setSleep(time: Long): Boolean {
        var errorMsg = "SWM Sleep Command was NOT successfully delivered."
        try {
            val command = "SL S=$time"
            val atCmdSeq = arrayOf("$" + command + "*" + SwmCommandChecksum.get(command))
            Log.d(
                logTag,
                DateTimeUtils.getDateTime() + " - Attempting Sleep Command : " + TextUtils.join(
                    ", ",
                    atCmdSeq
                )
            )
            val atCmdResponseLines =
                ShellCommands.executeCommandAsRoot(atCmdExecStr(ttyPath, busyBoxBin, atCmdSeq))
            for (atCmdResponseLine in atCmdResponseLines) {
                if (atCmdResponseLine.contains("OK")) {
                    Log.i(
                        logTag,
                        DateTimeUtils.getDateTime() + " - Sleep Command was successfully delivered "
                    )
                    return true
                } else if (atCmdResponseLine.contains("WAKE")) {
                    errorMsg = "Swarm has woken from sleep mode (getting message while sleeping)"
                }
            }
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        }
        Log.e(logTag, errorMsg)
        return false
    }

    companion object {
        private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmUtils")
        private const val baudRate = 115200
        const val sendCmdTimeout: Long = 70000
        const val prepCmdTimeout: Long = 2500
        const val powerCycleAfterThisManyConsecutiveDeliveryFailures = 5
        private fun atCmdExecStr(
            ttyPath: String?,
            busyBoxBin: String?,
            execSteps: Array<String>
        ): String {
            val execFull = StringBuilder()
            execFull.append(busyBoxBin).append(" stty -F ").append(ttyPath).append(" ").append(
                baudRate
            ).append(" cs8 -cstopb -parenb")
            for (i in execSteps.indices) {
                execFull.append(" && ")
                    .append("echo").append(" -n").append(" '").append(execSteps[i]).append("'")
                    .append(" | ")
                    .append(busyBoxBin).append(" microcom -t ").append(prepCmdTimeout)
                    .append(" -s ").append(
                        baudRate
                    ).append(" ").append(ttyPath)
            }
            return execFull.toString()
        }

        // Incoming Message Tools
        @Throws(JSONException::class)
        fun processIncomingSwm(smsObj: JSONObject, context: Context) {
            val app = context.applicationContext as RfcxGuardian

            // In this case, the message arrived from the API SMS address, so we attempt to parse it
            Log.w(logTag, "SWM received from API ''.")
            val segmentPayload = smsObj.getString("body")
            val swmSegmentReceivedResponse = app.resolver.query(
                RfcxComm.getUri(
                    "guardian",
                    "segment_receive_swm",
                    RfcxComm.urlEncode(segmentPayload)
                ),
                RfcxComm.getProjection("guardian", "segment_receive_swm"),
                null, null, null
            )
            swmSegmentReceivedResponse?.close()
        }

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

        fun addImmediateSwmToQueue(msgPayload: String?, context: Context): Boolean {
            return addScheduledSwmToQueue(System.currentTimeMillis(), msgPayload, context, true)
        }
    }

    init {
        app = context.applicationContext as RfcxGuardian
    }
}
