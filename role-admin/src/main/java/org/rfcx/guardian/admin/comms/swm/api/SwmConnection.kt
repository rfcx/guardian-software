package org.rfcx.guardian.admin.comms.swm.api

import android.util.Log

class SwmConnection(private val shell: SwmShell) {

    private val messageRegex = "\\$([A-Z]+ [^\\$]*)\\*([0-9a-fA-F]{2})$".toRegex()

    fun execute(command: String, arguments: String, timeout: Int = 1): List<String> {
        val request = makeRequest(command, arguments)
        val responseLines = shell.execute(request, timeout)
        return findMatchingResponses(responseLines, command)
    }

    fun executeWithoutTimeout(command: String, arguments: String): List<String> {
        val request = makeRequest(command, arguments)
        val responseLines = shell.execute(request)
        return findMatchingResponses(responseLines, command)
    }

    private fun makeRequest(command: String, arguments: String): String {
        val body = "$command $arguments"
        val checksum = SwmCommandChecksum.get(body)
        return "$${body}*${checksum}"
    }

    private fun findMatchingResponses(responses: List<String>, command: String): List<String> {
        return responses
            .mapNotNull { messageRegex.find(it) }
            .filter { match ->
                val (body, checksum) = match.destructured
                // Matches command and the checksum is valid
                body.startsWith("$command ") && SwmCommandChecksum.verify(body, checksum)
            }
            .map { match ->
                // Return only the body, removing the command and space
                match.groupValues[1].substring(command.length + 1)
            }
    }
}
