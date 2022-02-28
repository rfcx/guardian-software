package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SwmApiDTTest {

    @Test
    fun canGetDateTime() {
        // Arrange
        val shell = SwmMockShell(listOf("\$DT 20190408195123,V*41"))
        val api = SwmApi(SwmConnection(shell))

        // Act
        val dateTime = api.getDateTime()

        // Assert
        assertNotNull(dateTime)
        assertEquals(1554753083000, dateTime.epochMs)
    }
}
