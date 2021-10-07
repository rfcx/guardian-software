package org.rfcx.guardian.admin.comms.swm

class SwmMockShell(private val alwaysReturn: List<String> = listOf()): SwmShell {
    override fun execute(request: String, timeout: Int): List<String> {
        return alwaysReturn
    }
}
