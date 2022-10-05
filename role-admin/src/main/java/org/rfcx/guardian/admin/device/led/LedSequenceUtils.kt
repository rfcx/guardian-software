package org.rfcx.guardian.admin.device.led

import org.rfcx.guardian.utility.misc.ShellCommands

class LedSequenceUtils {

    fun ledOn() {
        setLedState(1)
    }

    fun ledOff() {
        setLedState(0)
    }

    /**
     * state = 1 : LED on
     * state = 0 : LED off
     */
    private fun setLedState(state: Int) {
        val command = "echo $state > /sys/class/leds/lcd-backlight/brightness"
        ShellCommands.executeCommand(command)
    }
}
