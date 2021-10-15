package org.rfcx.guardian.admin.comms.swm

import org.rfcx.guardian.admin.comms.swm.api.SwmShell

class SwmMockShell(private val alwaysReturn: List<String> = listOf()) : SwmShell {

    override fun execute(request: String): List<String> {
        return alwaysReturn
    }

}
