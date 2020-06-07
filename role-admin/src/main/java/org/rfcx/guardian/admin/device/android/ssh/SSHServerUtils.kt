package org.rfcx.guardian.admin.device.android.ssh

import android.content.Context
import org.rfcx.guardian.utility.misc.ShellCommands

object SSHServerUtils {

    fun startServer(context: Context) {
        //start sshd < cli
        ShellCommands.executeCommandAsRoot("start sshd", context)
    }

    fun stopServer(context: Context) {
        //stop sshd < cli
        ShellCommands.executeCommandAsRoot("stop sshd", context)
    }
}