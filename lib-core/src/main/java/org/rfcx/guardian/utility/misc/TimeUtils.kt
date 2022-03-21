package org.rfcx.guardian.utility.misc

import java.util.*

object TimeUtils {
    fun isCaptureAllowedAtThisTimeOfDay(timeRange: String): Boolean {
        "(-?[0-9]+:-?[0-9]+--?[0-9]+:-?[0-9]+)".toRegex().findAll(timeRange).let { result ->
            result.forEach { match ->
                val (time) = match.destructured
                val offHours = time.split("-")
                if (DateTimeUtils.isTimeStampWithinTimeRange(Date(), offHours[0], offHours[1])) {
                    return false
                }
            }
        }
        return true
    }
}
