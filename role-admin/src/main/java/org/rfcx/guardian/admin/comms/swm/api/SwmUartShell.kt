package org.rfcx.guardian.admin.comms.swm.api

import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.device.hardware.DeviceHardware_OrangePi_3G_IOT
import org.rfcx.guardian.utility.misc.FileUtils
import org.rfcx.guardian.utility.misc.ShellCommands
import org.rfcx.guardian.utility.rfcx.RfcxLog

class SwmUartShell(
    private val ttyPath: String = DeviceHardware_OrangePi_3G_IOT.DEVICE_TTY_FILEPATH_SATELLITE,
    private val busyboxBin: String = DeviceHardware_OrangePi_3G_IOT.BUSYBOX_FILEPATH,
    private val baudRate: Int = 115200
) : SwmShell {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmUartShell")

    init {
        if (!FileUtils.exists(busyboxBin)) {
            Log.e(logTag, "Busybox binary not found on system")
        }
        ShellCommands.executeCommandAsRoot(makeSerialPortSetupCommand(), false)
    }

    /**
     * Execute a command on tty and read the returned responses (one per line)
     */
    override fun execute(request: String, timeout: Int): List<String> {
        val ttyCommand = makeTtyCommand(request, timeout)
        try {
            return ShellCommands.executeCommandAsRoot(ttyCommand, false)
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        }
        return listOf()
    }

    private fun makeTtyCommand(input: String, timeout: Int = 1): String {
        return "echo -n '${input}\\r' > $ttyPath | $busyboxBin timeout $timeout /system/bin/cat < $ttyPath"
    }

    private fun makeSerialPortSetupCommand(): String {
        return "$busyboxBin stty -F $ttyPath $baudRate -echo -onlcr cs8 -cstopb -parenb"
    }
}
