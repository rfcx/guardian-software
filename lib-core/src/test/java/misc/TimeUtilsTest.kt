package misc

import org.junit.Test
import org.rfcx.guardian.utility.misc.TimeUtils
import java.util.*
import kotlin.test.assertEquals

class TimeUtilsTest {

    @Test
    fun currentTimeWithinOffHours() {
        // Arrange
        val timeRange = "00:00-23:59"
        val expectOutput = false

        // Act
        val result = TimeUtils.isCaptureAllowedAtThisTimeOfDay(timeRange)

        // Assert
        assertEquals(expectOutput, result)
    }

    @Test
    fun currentTimeNotWithinOffHours() {
        // Arrange
        val timeRange = "00:00-00:01"
        val expectOutput = true

        // Act
        val result = TimeUtils.isCaptureAllowedAtThisTimeOfDay(timeRange)

        // Assert
        assertEquals(expectOutput, result)
    }

    @Test
    fun canGetTimeRangeFromCorrectFormat() {
        // Arrange
        val timeRange = "00:00-00:01,00:02-00:03"
        val expectOutput = listOf("00:00-00:01", "00:02-00:03")

        // Act
        val result = TimeUtils.timeRangeToList(timeRange)

        // Assert
        assertEquals(expectOutput[0], result[0])
        assertEquals(expectOutput[1], result[1])
    }

    @Test
    fun canGetTimeRangeFromWrongFormat1() {
        // Arrange
        val timeRange = "00:00-00:01,00:0200:03"
        val expectOutput = listOf("00:00-00:01")

        // Act
        val result = TimeUtils.timeRangeToList(timeRange)

        // Assert
        assertEquals(expectOutput[0], result[0])
    }

    @Test
    fun canGetTimeRangeFromWrongFormat2() {
        // Arrange
        val timeRange = ",,00:00-00:01,00:02-00:03"
        val expectOutput = listOf("00:00-00:01", "00:02-00:03")

        // Act
        val result = TimeUtils.timeRangeToList(timeRange)

        // Assert
        assertEquals(expectOutput[0], result[0])
        assertEquals(expectOutput[1], result[1])
    }

    @Test
    fun canGetTimeRangeFromWrongFormat3() {
        // Arrange
        val timeRange = ",,00:00:00:00-00:01,00:02-00:03-:30"
        val expectOutput = listOf("00:00-00:01", "00:02-00:03")

        // Act
        val result = TimeUtils.timeRangeToList(timeRange)

        // Assert
        assertEquals(expectOutput[0], result[0])
        assertEquals(expectOutput[1], result[1])
    }
}
