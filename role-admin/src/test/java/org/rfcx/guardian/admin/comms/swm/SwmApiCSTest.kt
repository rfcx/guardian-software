package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SwmApiCSTest {

    @Test
    fun canGetSwarmId() {
        // Arrange
        val shell = SwmMockShell(listOf("\$CS DI=0x000e57,DN=TILE*10"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val deviceId = api.getSwarmDeviceId()

        // Assert
        assertNotNull(deviceId)
        assertEquals("0x000e57", deviceId)
    }

}
