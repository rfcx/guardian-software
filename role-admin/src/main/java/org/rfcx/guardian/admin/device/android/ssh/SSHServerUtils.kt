package org.rfcx.guardian.admin.device.android.ssh

import android.content.Context
import org.rfcx.guardian.utility.misc.ShellCommands

object SSHServerUtils {

    fun startServer() {
        //start sshd < cli
        ShellCommands.executeCommandAsRoot("start sshd")
    }

    fun stopServer() {
        //stop sshd < cli
        ShellCommands.executeCommandAsRoot("stop sshd")
    }
}