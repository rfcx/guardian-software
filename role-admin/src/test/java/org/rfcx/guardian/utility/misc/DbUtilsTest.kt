package org.rfcx.guardian.utility.misc

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DbUtilsTest {

    @Test
    fun canCompressNullSatellite() {
        // Arrange
        val expectOutput = listOf(
            "1634289349000*-87*0",
            "1634289349257*-87*-90*-8*-1263*2021-10-15 08:26:02*0x00055*0"
        )
        val diagnosticRows = listOf(
            arrayOf("1634289349000", "-87", null, null, null, null, null, "0"),
            arrayOf(
                "1634289349257",
                "-87",
                "-90",
                "-8",
                "-1263",
                "2021-10-15 08:26:02",
                "0x00055",
                "0"
            )
        )

        // Act
        val arr = DbUtils.getConcatRowsIgnoreNullSatellite(diagnosticRows).split("|")

        // Assert
        assertEquals(expectOutput[0], arr[0])
        assertEquals(expectOutput[1], arr[1])
    }

    @Test
    fun canCompressNullSatelliteOnlyOneItemWithNull() {
        // Arrange
        val expectOutput = listOf(
            "1634289349000*-87*0"
        )
        val diagnosticRows = listOf(
            arrayOf("1634289349000", "-87", null, null, null, null, null, "0")
        )

        // Act
        val arr = DbUtils.getConcatRowsIgnoreNullSatellite(diagnosticRows).split("|")

        // Assert
        assertEquals(expectOutput[0], arr[0])
    }

    @Test
    fun canCompressNullSatelliteOnlyOneItemWithFull() {
        // Arrange
        val expectOutput = listOf(
            "1634289349257*-87*-90*-8*-1263*2021-10-15 08:26:02*0x00055*0"
        )
        val diagnosticRows = listOf(
            arrayOf(
                "1634289349257",
                "-87",
                "-90",
                "-8",
                "-1263",
                "2021-10-15 08:26:02",
                "0x00055",
                "0"
            )
        )

        // Act
        val arr = DbUtils.getConcatRowsIgnoreNullSatellite(diagnosticRows).split("|")

        // Assert
        assertEquals(expectOutput[0], arr[0])
    }

    @Test
    fun canCompressNullSatelliteEmptyItem() {
        // Arrange
        val diagnosticRows = listOf<Array<String>>()

        // Act
        val arr = DbUtils.getConcatRowsIgnoreNullSatellite(diagnosticRows)

        // Assert
        assertNull(arr)
    }

    @Test
    fun canIgnoreNullSensor() {
        // Arrange
        val sensorRows = listOf(
            arrayOf("1634289349000", "0"),
            arrayOf(
                "1634289349257",
                "0",
                "0",
                "0"
            )
        )

        // Act
        val arr = DbUtils.getConcatRowsIgnoreNullSensors("sensor", sensorRows)

        // Assert
        assertNull(arr)
    }

    @Test
    fun canIgnoreNullSensorOnEmptyArr() {
        // Arrange
        val sensorRows = listOf<Array<String>>()

        // Act
        val arr = DbUtils.getConcatRowsIgnoreNullSensors("sensor", sensorRows)

        // Assert
        assertNull(arr)
    }
}
