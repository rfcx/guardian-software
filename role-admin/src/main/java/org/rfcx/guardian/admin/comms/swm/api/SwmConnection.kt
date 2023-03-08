package org.rfcx.guardian.admin.comms.swm.api

import android.os.Environment
import org.rfcx.guardian.utility.misc.DateTimeUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

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
        val responsesMatch = responses
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
        commandToLog(command, responsesMatch)
        return responsesMatch
    }


    private fun commandToLog(command: String, result: List<String>) {
        val logFile = File(Environment.getExternalStorageDirectory().absolutePath + "/SwarmLog.txt")
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        try {
            val buf = BufferedWriter(FileWriter(logFile, true))
            buf.append("${DateTimeUtils.getDateTime()}:$command ,,,$result")
            buf.newLine()
            buf.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
