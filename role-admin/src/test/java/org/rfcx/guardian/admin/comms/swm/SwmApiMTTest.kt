package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import org.rfcx.guardian.admin.comms.swm.data.SwmUnsentMsg
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

    @Test
    fun canGetAllUnsentMessages() {
        // Arrange
        val expectResults = listOf(
            "\$MT 68692066726f6d20737761726d,4428826476689,1605639598*55",
            "\$MT 686f6c6120646573646520737761726d,4428826476690,1605639664*53"
        )
        val expectOutputs = listOf(
            SwmUnsentMsg("68692066726f6d20737761726d", "4428826476689", "1605639598"),
            SwmUnsentMsg("686f6c6120646573646520737761726d", "4428826476690", "1605639664")
        )
        val shell = SwmMockShell(expectResults)
        val api = SwmApi(SwmConnection(shell))

        // Act
        val unsentMessages = api.getUnsentMessages()

        // Assert
        assertNotNull(unsentMessages)
        assertEquals(2, unsentMessages.size)
        unsentMessages.forEachIndexed { index, unsentMessage ->
            assertEquals(expectOutputs[index].hex, unsentMessage.hex)
            assertEquals(expectOutputs[index].messageId, unsentMessage.messageId)
            assertEquals(expectOutputs[index].timestamp, unsentMessage.timestamp)
        }
    }
}
