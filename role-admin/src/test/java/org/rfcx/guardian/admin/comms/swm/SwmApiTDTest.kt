package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SwmApiTDTest {

    @Test
    fun canSendMessage() {
        // Arrange
        val shell = SwmMockShell(listOf("\$TD OK,5354468575855*2a"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val messageId = api.transmitData("") // no need to add any message here since we mocked Shell class

        // Assert
        assertNotNull(messageId)
        assertEquals("5354468575855", messageId)
    }

    @Test
    fun canSendMessageButError() {
        // Arrange
        val shell = SwmMockShell(listOf("\$TD ERR,BADDATA*0e"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val messageId = api.transmitData("") // no need to add any message here since we mocked Shell class

        // Assert
        assertNull(messageId)
    }
}
