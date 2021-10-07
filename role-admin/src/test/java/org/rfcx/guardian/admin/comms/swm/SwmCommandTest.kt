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
        assertEquals(Date.parse("2021-10-01 12:13:14"), dateTime.time)
    }

    @Test
    fun canGetSignalBackgroundOnly() {
        // Arrange
        val shell = SwmMockShell(listOf("\$RT RSSI=-101*1d"))
        val command = SwmCommand(shell)

        // Act
        val signal = command.getSignal()

        // Assert
        assertNotNull(signal)
        assertEquals(signal.rssiBackground, -101)
        assertNull(signal.rssiSatellite)
    }

    @Test
    fun canGetSignalBackgroundOnlyTakeFirst() {
        // Arrange
        val shell = SwmMockShell(listOf("\$RT RSSI=-103*1f", "\$RT RSSI=-102*1e", "\$RT RSSI=-101*1d"))
        val command = SwmCommand(shell)

        // Act
        val signal = command.getSignal()

        // Assert
        assertNotNull(signal)
        assertEquals(signal.rssiBackground, -103)
        assertNull(signal.rssiSatellite)
    }

    @Test
    fun canGetAllSignal() {
        // Arrange
        val shell = SwmMockShell(listOf("\$RT RSSI=-103*1f", "\$RT RSSI=-102,SNR=-1,FDEV=426,TS=2020-10-02 13:56:21,DI=0x000568*04"))
        val command = SwmCommand(shell)

        // Act
        val signal = command.getSignal()

        // Assert
        assertNotNull(signal)
        assertEquals(signal.rssiBackground, -103)
        assertEquals(signal.rssiSatellite, -1)
        assertEquals(signal.signalToNoiseRatio, -1)
        assertEquals(signal.frequencyDeviation, -1)
        assertEquals(signal.packetTimestamp, "2020-10-02 13:56:21")
        assertEquals(signal.satelliteId, "0x000568")

    }
}
