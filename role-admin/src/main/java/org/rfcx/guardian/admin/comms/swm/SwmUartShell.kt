package org.rfcx.guardian.admin.comms.swm

import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.misc.FileUtils
import org.rfcx.guardian.utility.misc.ShellCommands
import org.rfcx.guardian.utility.rfcx.RfcxLog
import java.lang.Exception

class SwmUartShell(private val ttyPath: String, private val busyboxBin: String, private val baudRate: Int): SwmShell {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmUartShell")

    /**
     * Execute a command on tty and read the returned responses (one per line)
     */
    override fun execute(request: String): List<String>? {
        val ttyCommand = makeTtyCommand(request)
        var errorMsg = "$request was NOT successfully delivered."
        try {
            if (!FileUtils.exists(busyboxBin)) {
                errorMsg = "BusyBox binary not found on system"
            } else {
                return ShellCommands.executeCommandAsRoot(ttyCommand)
            }
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        }
        Log.e(logTag, errorMsg)
        return null
    }

    private fun makeTtyCommand(input: String): String {
        val timeout = 1000 // TODO make this a param or class var
        val echo = "echo -n '${input}' | $busyboxBin microcom -t $timeout -s $baudRate $ttyPath"
        return "$echo"
    }
}
