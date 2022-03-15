package org.rfcx.guardian.utility.misc

import java.util.*

object TimeUtils {
    fun isCaptureAllowedAtThisTimeOfDay(timeRange: String): Boolean {
        for (offHoursRange in timeRange.split(",").filter { it != "" }) {
            val offHours = offHoursRange.split("-").filter { it != "" }
            if (offHours.isEmpty()) continue
            if (DateTimeUtils.isTimeStampWithinTimeRange(Date(), offHours[0], offHours[1])) {
                return false
            }
        }
        return true
    }
}
