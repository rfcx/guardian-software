package org.rfcx.guardian.admin.comms.swm

import org.rfcx.guardian.utility.misc.ShellCommands

class SwmUartShell(val ttyPath: String, val busyboxBin: String, val baudRate: String): SwmShell {
    /**
     * Execute a command on tty and read the returned responses (one per line)
     */
    override fun execute(request: String): List<String> {
        val ttyCommand = makeTtyCommand(request)
        return ShellCommands.executeCommandAsRoot(ttyCommand)
    }

    private fun makeTtyCommand(input: String): String {
        val timeout = 1000 // TODO make this a param or class var
        val stty = "${busyboxBin} stty -F ${ttyPath} ${baudRate} cs8 -cstopb -parenb && "
        val echo = "echo -n '${input}' | ${busyboxBin} microcom -t ${timeout} -s ${baudRate} ${ttyPath}"
        return "${stty} && ${echo}"
    }
}