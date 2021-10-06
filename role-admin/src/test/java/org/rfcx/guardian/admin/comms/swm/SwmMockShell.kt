package org.rfcx.guardian.admin.comms.swm

class SwmMockShell(val alwaysReturn: List<String> = listOf()): SwmShell {
    override fun execute(request: String): List<String> {
        return alwaysReturn
    }
}