package org.rfcx.guardian.admin.comms.swm

interface SwmShell {
    /**
     * Send a request to the Swarm board and
     * retrieve the string response
     */
    fun execute(request: String, timeout: Int = 1): List<String>

}
