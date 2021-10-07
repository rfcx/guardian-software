package org.rfcx.guardian.admin.comms.swm

import org.rfcx.guardian.admin.comms.swm.data.SwmRT
import java.util.*

class SwmCommand(private val shell: SwmShell) {

    enum class SwarmCommand { TD, MT, SL, RT, DT }

    private fun execute(command: SwarmCommand, arguments: String): List<String> {
        val request = makeRequest(command.name, arguments)
        val responseLines = shell.execute(request)
        return findResponseMatching(responseLines, command)
    }

    fun transmitData(msgStr: String): List<String> {
        return execute(SwarmCommand.TD, msgStr)
    }

    fun getUnsentMessages(): List<String> {
        return execute(SwarmCommand.MT, "L=U")
    }

    fun sleep(time: Long): List<String> {
        return execute(SwarmCommand.SL, "S=$time")
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
