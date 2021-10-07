package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import java.util.*
import kotlin.test.*

class SwmCommandTest {

    @Test
    fun canGetDateTime() {
        // Arrange
        val shell = SwmMockShell(listOf("\$TILE hello","\$DT 20211001121314,V*XX"))
        val command = SwmCommand(shell)

        // Act
        val dateTime = command.getDateTime()

        // Assert
        assertEquals(Date.parse("2021-10-01 12:13:14"), dateTime?.time)
    }

    @Test
    fun canGetSignalBackgroundOnly() {
        // Arrange
        val shell = SwmMockShell(listOf("\$RT RSSI=-101*1d"))
        val command = SwmCommand(shell)

        // Act
        val rtBackground = command.getRTBackground()

        // Assert
        assertNotNull(rtBackground)
        assertEquals(rtBackground.rssi, -101)
    }

    @Test
    fun canGetSignalBackgroundCombineWithOKResponse() {
        // Arrange
        val shell = SwmMockShell(listOf("\$RT OK*23", "\$RT RSSI=-102*1e"))
        val command = SwmCommand(shell)

        // Act
        val rtBackground = command.getRTBackground()

        // Assert
        assertNotNull(rtBackground)
        assertEquals(rtBackground.rssi, -102)
    }

    @Test
    fun canGetSignalBackgroundOnlyTakeFirst() {
        // Arrange
        val shell = SwmMockShell(listOf("\$RT RSSI=-103*1f", "\$RT RSSI=-102*1e", "\$RT RSSI=-101*1d"))
        val command = SwmCommand(shell)

        // Act
        val rtBackground = command.getRTBackground()

        // Assert
        assertNotNull(rtBackground)
        assertEquals(rtBackground.rssi, -103)
    }

    @Test
    fun canGetAllSignal() {
        // Arrange
        val shell = SwmMockShell(listOf("\$RT RSSI=-103*1f", "\$RT RSSI=-102,SNR=-1,FDEV=426,TS=2020-10-02 13:56:21,DI=0x000568*04"))
        val command = SwmCommand(shell)

        // Act
        val rtSatellite = command.getRTSatellite()

        // Assert
        assertNotNull(rtSatellite)
        assertEquals(rtSatellite.rssi, -102)
        assertEquals(rtSatellite.signalToNoiseRatio, -1)
        assertEquals(rtSatellite.frequencyDeviation, 426)
        assertEquals(rtSatellite.packetTimestamp, "2020-10-02 13:56:21")
        assertEquals(rtSatellite.satelliteId, "0x000568")

    }
}
