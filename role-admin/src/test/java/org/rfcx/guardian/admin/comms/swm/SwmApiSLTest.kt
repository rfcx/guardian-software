package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import kotlin.test.assertEquals

class SwmApiSLTest {

    @Test
    fun canDetectSleepSuccess() {
        // Arrange
        val shell = SwmMockShell(listOf("\$SL OK*3b"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val sleepResponse = api.sleep()

        // Assert
        assertEquals(true, sleepResponse)
    }

    @Test
    fun canDetectSleepError() {
        // Arrange
        val shell = SwmMockShell(listOf("\$SL ERR*7a"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val sleepResponse = api.sleep()

        // Assert
        assertEquals(false, sleepResponse)
    }

    @Test
    fun canDetectSleepNoResponse() {
        // Arrange
        val shell = SwmMockShell(listOf())
        val api = SwmApi(SwmConnection(shell))

        // Act
        val sleepResponse = api.powerOff()

        // Assert
        assertEquals(false, sleepResponse)
    }
}
