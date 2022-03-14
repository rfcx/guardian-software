package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SwmApiRTTest {

    @Test
    fun canGetRTBackgroundWhenNoResponse() {
        // Arrange
        val shell = SwmMockShell(listOf())
        val api = SwmApi(SwmConnection(shell))

        // Act
        val rtBackground = api.getRTBackground()

        // Assert
        assertNull(rtBackground)
    }

    @Test
    fun canGetRTBackground() {
        // Arrange
        val shell = SwmMockShell(listOf("\$RT RSSI=-101*1d"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val rtBackground = api.getRTBackground()

        // Assert
        assertNotNull(rtBackground)
        assertEquals(rtBackground.rssi, -101)
    }

    @Test
    fun canGetRTBackgroundCombineWithOKResponse() {
        // Arrange
        val shell = SwmMockShell(listOf("\$RT OK*23", "\$RT RSSI=-102*1e"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val rtBackground = api.getRTBackground()

        // Assert
        assertNotNull(rtBackground)
        assertEquals(rtBackground.rssi, -102)
    }

    @Test
    fun canGetRTBackgroundFirstResultFromMany() {
        // Arrange
        val shell =
            SwmMockShell(listOf("\$RT RSSI=-103*1f", "\$RT RSSI=-102*1e", "\$RT RSSI=-101*1d"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val rtBackground = api.getRTBackground()

        // Assert
        assertNotNull(rtBackground)
        assertEquals(rtBackground.rssi, -103)
    }

    @Test
    fun canGetRT() {
        // Arrange
        val shell = SwmMockShell(
            listOf(
                "\$RT RSSI=-103*1f",
                "\$RT RSSI=-102,SNR=-1,FDEV=426,TS=2020-10-02 13:56:21,DI=0x000568*04"
            )
        )
        val api = SwmApi(SwmConnection(shell))

        // Act
        val rtSatellite = api.getRTSatellite()

        // Assert
        assertNotNull(rtSatellite)
        assertEquals(rtSatellite.rssi, -102)
        assertEquals(rtSatellite.signalToNoiseRatio, -1)
        assertEquals(rtSatellite.frequencyDeviation, 426)
        assertEquals(rtSatellite.packetTimestamp, "2020-10-02 13:56:21")
        assertEquals(rtSatellite.satelliteId, "0x000568")
    }
}
