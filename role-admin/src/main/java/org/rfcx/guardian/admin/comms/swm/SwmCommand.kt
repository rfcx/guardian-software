package org.rfcx.guardian.admin.comms.swm

import org.rfcx.guardian.admin.comms.swm.data.*
import java.util.*

class SwmCommand(private val shell: SwmShell) {

    enum class SwarmCommand { TD, MT, SL, RT, DT }

    private fun execute(command: SwarmCommand, arguments: String): List<String> {
        val request = makeRequest(command.name, arguments)
        val responseLines = shell.execute(request)
        return findResponseMatching(responseLines, command)
    }

    fun transmitData(msgStr: String): SwmTD? {
        return execute(SwarmCommand.TD, msgStr).firstOrNull()?.let { payload ->
            return "OK,(-?[0-9]+)".toRegex().find(payload)?.let { result ->
                val (id) = result.destructured
                return SwmTD(messageId = id)
            }
        }
    }

    fun getUnsentMessages(): SwmMT? {
        val unsentMessages = execute(SwarmCommand.MT, "L=U")
        val unsentMsgIds = arrayListOf<SwmUnsentMsg>()
        if (unsentMessages.isEmpty()) return null
        unsentMessages.forEach { payload ->
            "OK,(-?[0-9]+)".toRegex().find(payload)?.let { result ->
                val (id) = result.destructured
                unsentMsgIds.add(SwmUnsentMsg("", id))
            }
        }
        return SwmMT(unsentMsgIds)
    }

    fun sleep(time: Long): Boolean? {
        return execute(SwarmCommand.SL, "S=$time").firstOrNull()?.let { payload ->
            return payload.contains("OK")
        }
    }

    fun getSignal(): SwmRT? {
        return execute(SwarmCommand.RT, "@").firstOrNull()?.let { payload ->
            return "RSSI=(-?[0-9]+)".toRegex().find(payload)?.let { result ->
                val (rssi) = result.destructured
                return SwmRT(rssiBackground = rssi.toInt())
            }
        }
    }

    fun getDateTime(): Date? {
        val response = execute(SwarmCommand.DT, "@")
        // TODO parse result
        return Date()
    }

    private fun makeRequest(command: String, arguments: String): String {
        val body = "$command $arguments"
        val checksum = SwmCommandChecksum.get(body)
        return "$${body}*${checksum}"
    }

    private fun findResponseMatching(responses: List<String>, command: SwarmCommand): List<String> {
        return responses.filter { it.contains(command.name) }
    }
}
