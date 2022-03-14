package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import org.rfcx.guardian.admin.comms.swm.api.SwmConnection
import kotlin.test.assertEquals

class SwmConnectionTest {

    @Test
    fun canReturnSingleValidMessages() {
        // Arrange
        val shell = SwmMockShell(listOf("\$GS OK*30"))
        val connection = SwmConnection(shell)

        // Act
        val responses = connection.execute("GS", "1")

        // Assert
        assertEquals(1, responses.size)
        assertEquals("OK", responses[0])
    }

    @Test
    fun canReturnMultipleValidMessages() {
        // Arrange
        val shell = SwmMockShell(
            listOf(
                "\$GN 15*2d",
                "\$GN 37.8921,-122.0155,77,89,2*01",
                "\$GN 37.8921,-122.0155,77,89,3*00"
            )
        )
        val connection = SwmConnection(shell)

        // Act
        val responses = connection.execute("GN", "?")

        // Assert
        assertEquals(3, responses.size)
        assertEquals("15", responses[0])
    }

    @Test
    fun canFilterUnrelatedCommands() {
        // Arrange
        val shell = SwmMockShell(listOf("\$GJ OK*29", "\$DT 20190408195123,V*41"))
        val connection = SwmConnection(shell)

        // Act
        val responses = connection.execute("DT", "ANY")

        // Assert
        assertEquals(1, responses.size)
        assertEquals("20190408195123,V", responses[0])
    }

    @Test
    fun canFilterInvalidChecksums() {
        // Arrange
        val invalidMessage = "\$DT 20190408195124,V*41"
        val validMessage = "\$DT 20190408195123,V*41"
        val shell = SwmMockShell(listOf(invalidMessage, validMessage))
        val connection = SwmConnection(shell)

        // Act
        val responses = connection.execute("DT", "ANY")

        // Assert
        assertEquals(1, responses.size)
        assertEquals("20190408195123,V", responses[0])
    }

}
