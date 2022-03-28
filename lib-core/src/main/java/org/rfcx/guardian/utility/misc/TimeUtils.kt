package org.rfcx.guardian.utility.misc

import java.util.*

object TimeUtils {

    fun timeRangeToList(timeRange: String): List<String> {
        val timeList = arrayListOf<String>()
        "([0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2})".toRegex().findAll(timeRange).let { result ->
            result.forEach { match ->
                val (time) = match.destructured
                timeList.add(time)
            }
        }
        return timeList
    }

    fun isDateOutsideTimeRange(date: Date, timeRange: String): Boolean {
        val timeList = timeRangeToList(timeRange)
        timeList.forEach { time ->
            val offHours = time.split("-")
            if (DateTimeUtils.isTimeStampWithinTimeRange(date, offHours[0], offHours[1])) {
                return false
            }
        }
        return true
    }

    fun isNowOutsideTimeRange(timeRange: String): Boolean = isDateOutsideTimeRange(Date(), timeRange)
}
