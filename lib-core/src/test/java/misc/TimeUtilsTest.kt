package misc

import org.junit.Test
import org.rfcx.guardian.utility.misc.TimeUtils
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TimeUtilsTest {

    @Test
    fun currentTimeWithinOffHours() {
        // Arrange
        val timeRange = "01:00-01:59"
        val date = GregorianCalendar(2022, Calendar.FEBRUARY, 11, 1, 25, 34).time

        // Act
        val result = TimeUtils.isDateOutsideTimeRange(date, timeRange)

        // Assert
        assertFalse(result)
    }

    @Test
    fun currentTimeNotWithinOffHours() {
        // Arrange
        val timeRange = "00:00-00:01"
        val expectOutput = true

        // Act
        val result = TimeUtils.isNowOutsideTimeRange(timeRange)

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
