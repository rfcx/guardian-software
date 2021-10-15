package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SwmApiMTTest {

    @Test
    fun canCountUnsentMessages() {
        // Arrange
        val shell = SwmMockShell(listOf("\$MT 12*3a"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val unsentCount = api.getNumberOfUnsentMessages()

        // Assert
        assertNotNull(unsentCount)
        assertEquals(12, unsentCount)
    }

    @Test
    fun canCountUnsentMessagesWithZeroCount() {
        // Arrange
        val shell = SwmMockShell(listOf("\$MT 0*09"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val unsentCount = api.getNumberOfUnsentMessages()

        // Assert
        assertNotNull(unsentCount)
        assertEquals(0, unsentCount)
    }

    @Test
    fun canCountUnsentMessagesWithErr() {
        // Arrange
        val shell = SwmMockShell(listOf("\$MT ERR,BADPARAM*58"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val unsentCount = api.getNumberOfUnsentMessages()

        // Assert
        assertNotNull(unsentCount)
        assertEquals(0, unsentCount)
    }

    @Test
    fun canCountUnsentMessagesWithMultipleOutput() {
        // Arrange
        val shell = SwmMockShell(listOf("\$DT 20190408195123,V*41", "\$MT 12*3a"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val unsentCount = api.getNumberOfUnsentMessages()

        // Assert
        assertNotNull(unsentCount)
        assertEquals(12, unsentCount)
    }
}
