package org.rfcx.guardian.admin.comms.swm

import org.junit.Test
import kotlin.test.assertEquals

class SwmCommandChecksumTest {
    @Test
    fun canGet() {
        // Arrange
        val text = "DT @"

        // Act
        val checksum = SwmCommandChecksum.get(text)

        // Assert
        assertEquals("70", checksum)
    }

    @Test
    fun canVerifyValidChecksum() {
        val valid = SwmCommandChecksum.verify("DT @", "70")

        assertEquals(true, valid)
    }

    @Test
    fun canVerifyInvalidChecksum() {
        val valid = SwmCommandChecksum.verify("SL @", "70")

        assertEquals(false, valid)
    }

    @Test
    fun canVerifyInvalidLengthChecksum() {
        val valid1 = SwmCommandChecksum.verify("SL @", "")
        val valid2 = SwmCommandChecksum.verify("SL @", "hello!")

        assertEquals(false, valid1)
        assertEquals(false, valid2)
    }
}