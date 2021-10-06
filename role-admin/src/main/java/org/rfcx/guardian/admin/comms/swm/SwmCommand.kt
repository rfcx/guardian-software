package org.rfcx.guardian.admin.comms.swm

import java.util.*

class SwmCommand(val shell: SwmShell) {

    fun execute(command: String, arguments: String): String? {
        val request = makeRequest(command, arguments)
        val responseLines = shell.execute(request)
        return findResponseMatching(responseLines, command)
    }

    fun getDateTime(): Date {
        val response = execute("DT", "@")
        // TODO parse response and convert to date
        return Date()
    }

    private fun makeRequest(command: String, arguments: String): String {
        val body = "${command} ${arguments}"
        val checksum = SwmCommandChecksum.get(body)
        return "$${body}*${checksum}"
    }

    private fun findResponseMatching(responses: List<String>, command: String): String? {
        TODO("Not yet implemented")
    }
}