package org.rfcx.guardian.admin.comms.swm

import java.util.*

class SwmCommand(private val shell: SwmShell) {

    enum class SwarmCommand { TD, MT, SL, RT, DT }

    private fun execute(command: SwarmCommand, arguments: String): List<String>? {
        val request = makeRequest(command.name, arguments)
        val responseLines = shell.execute(request)
        return findResponseMatching(responseLines, command)
    }

    fun transmitData(msgStr: String): List<String>? {
        return execute(SwarmCommand.TD, "\""+msgStr+"\\\\r\"")
    }

    fun getUnsentMessages(): List<String>? {
        return execute(SwarmCommand.MT, "L=U")
    }

    fun sleep(time: Long): List<String>? {
        return execute(SwarmCommand.SL, "S=$time")
    }

    fun getRecentSignal(): List<String>? {
        return execute(SwarmCommand.RT, "@")
    }

    fun getDateTime(): Date {
        val response = execute(SwarmCommand.DT, "@")
        return Date()
    }

    private fun makeRequest(command: String, arguments: String): String {
        val body = "$command $arguments"
        val checksum = SwmCommandChecksum.get(body)
        return "$${body}*${checksum}"
    }

    private fun findResponseMatching(responses: List<String>?, command: SwarmCommand): List<String>? {
        if (responses == null) return null
        when (command) {
            SwarmCommand.TD -> {
                return responses.filter { it.contains(command.name) }
            }
            SwarmCommand.MT -> {
                return responses.filter { it.contains(command.name) }
            }
            SwarmCommand.SL -> {
                return responses.filter { it.contains(command.name) }
            }
            SwarmCommand.RT -> {
                return responses.filter { it.contains(command.name) }
            }
            SwarmCommand.DT -> {
                return responses.filter { it.contains(command.name) }
            }
        }
    }
}
