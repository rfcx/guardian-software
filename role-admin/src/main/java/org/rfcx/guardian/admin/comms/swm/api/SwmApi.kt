package org.rfcx.guardian.admin.comms.swm.api

import android.util.Log
import org.rfcx.guardian.admin.comms.swm.data.*
import java.text.SimpleDateFormat
import java.util.*

class SwmApi(private val connection: SwmConnection) {

    enum class Command { TD, MT, SL, PO, RT, DT }

    private val datetimeCompactFormatter = SimpleDateFormat("yyyyMMddHHmmss").also { it.timeZone = TimeZone.getTimeZone("GMT") }

    // TODO unit test
    fun transmitData(msgStr: String): SwmTDResponse? {

        // These two lines can be removed once the SwmTDResponse code below works properly
        connection.execute(Command.TD.name, msgStr).firstOrNull()
        return SwmTDResponse(messageId = "id")
        // These two lines can be removed once the SwmTDResponse code below works properly

//        return connection.execute(Command.TD.name, msgStr).firstOrNull()?.let { payload ->
//            return "\$TDOK,(-?[0-9]+)".toRegex().find(payload)?.let { result ->
//                val (id) = result.destructured
//                Log.d("SwmCommand", "TD= $id")
//                return SwmTDResponse(messageId = id)
//            }
//        }
    }

    // This is just a placeholder so that the command can be run.
    // Should be replaced with one that properly parses the feedback.
    fun getUnsentMessageCount(): SwmMTResponse? {
        val unsentMessageCount = connection.execute(Command.MT.name, "C=U").firstOrNull()
        val unsentMsgCnt = arrayListOf<SwmUnsentMsg>()
        return SwmMTResponse(unsentMsgCnt)
    }

    // TODO unit test
//    fun getUnsentMessages(): SwmMTResponse? {
//        val unsentMessages = connection.execute(Command.MT.name, "L=U")
//        val unsentMsgIds = arrayListOf<SwmUnsentMsg>()
//        if (unsentMessages.isEmpty()) return null
//        unsentMessages.forEach { payload ->
//            "OK,(-?[0-9]+)".toRegex().find(payload)?.let { result ->
//                val (id) = result.destructured
//                unsentMsgIds.add(SwmUnsentMsg("", id))
//            }
//        }
//        return SwmMTResponse(unsentMsgIds)
//    }

    // TODO unit test
//    fun sleep(time: Long): Boolean? {
//        return connection.execute(Command.SL.name, "S=$time").firstOrNull()?.let { payload ->
//            return payload.contains("OK")
//        }
//    }

    // TODO unit test
    fun powerOff(): Boolean? {
        return connection.execute(Command.PO.name, "", 10).firstOrNull()?.let { payload ->
            return payload.contains("OK")
        }
    }

    fun getRTSatellite(): SwmRTResponse? {
        val results = connection.execute(Command.RT.name, "@")
        val regex = "RSSI=(-?[0-9]+),SNR=(-?[0-9]+),FDEV=(-?[0-9]+),TS=([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}),DI=(0x[0-9]+)".toRegex()
        val firstMatchResult = results.mapNotNull { regex.find(it) }.firstOrNull()
        return firstMatchResult?.let { match ->
            val (rssi, snr, fdev, time, satId) = match.destructured
            return SwmRTResponse(rssi.toInt(), snr.toInt(), fdev.toInt(), time, satId)
        }
    }

    fun getRTBackground(): SwmRTBackgroundResponse? {
        // Set the background rate to 1s and wait 1.5s to get a result
        val result = connection.execute(Command.RT.name, "3", 6).firstOrNull { !it.contains("OK") }?.let { payload ->
            "RSSI=(-?[0-9]+)".toRegex().find(payload)?.let { match ->
                val (rssi) = match.destructured
                SwmRTBackgroundResponse(rssi = rssi.toInt())
            }
        }
        Log.d("RfcxSwmCommand", "RT RSSI=${result?.rssi}")
        // Set the rate back to off
        connection.execute(Command.RT.name, "0")
        return result
    }

    fun getDateTime(): SwmDTResponse? {
        val datetime = connection.execute(Command.DT.name, "@")
            .firstOrNull()?.let {
                val match = "^([0-9]{14}),V$".toRegex().find(it) ?: return null
                datetimeCompactFormatter.parse(match.groupValues[1])
            } ?: return null
        return SwmDTResponse(datetime.time)
    }
}
