package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

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

}