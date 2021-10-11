package org.rfcx.guardian.admin.comms.swm.api

interface SwmShell {
    /**
     * Send a request to the Swarm board and
     * retrieve the string response
     */
    fun execute(request: String, timeout: Int = 1): List<String>

    fun executeWithoutTimeout(request: String): List<String>

}
