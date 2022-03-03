package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SwmApiGSTest {

    @Test
    fun canGetGPSConnectionG3Type() {
        // Arrange
        val shell = SwmMockShell(listOf("\$GS 144,273,7,0,G3*40"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val gps = api.getGPSConnection()

        // Assert
        assertNotNull(gps)
        assertEquals(144, gps.hdop)
        assertEquals(273, gps.vdop)
        assertEquals(7, gps.gnss)
        assertEquals("G3", gps.type)
    }

    @Test
    fun canGetGPSConnectionTTType() {
        // Arrange
        val shell = SwmMockShell(listOf("\$GS 144,273,7,0,TT*34"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val gps = api.getGPSConnection()

        // Assert
        assertNotNull(gps)
        assertEquals(144, gps.hdop)
        assertEquals(273, gps.vdop)
        assertEquals(7, gps.gnss)
        assertEquals("TT", gps.type)
    }

    @Test
    fun cannotGetGPSConnection() {
        // Arrange
        val shell = SwmMockShell(listOf(""))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val gps = api.getGPSConnection()

        // Assert
        assertNull(gps)
    }
}
