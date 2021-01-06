package org.rfcx.guardian.guardian.utils

import org.rfcx.guardian.utility.misc.DateTimeUtils

class CheckInInformationUtils {

    private fun convertTimeStampToStringFormat(timestamp: Long?): String {
        if (timestamp != null) {
            var latestCheckInStr = ""
            val latestCheckIn = System.currentTimeMillis() - timestamp
            val minutes = latestCheckIn / 60000
            latestCheckInStr = if (minutes > 60L) {
                val hours = minutes / 60
                val min = minutes % 60
                if (min == 0L) {
                    "$hours hours"
                } else {
                    "$hours hours and $min minutes ago"
                }
            } else {
                "$minutes minutes ago"
            }

            return latestCheckInStr
        } else {
            return "none"
        }
    }

    private fun convertFileSizeToStringFormat(size: Long?): String {
        var audioSize = "-"
        if (size != null && size != 0L) {
            audioSize = (size.toLong() / 1000).toString()
        }
        return if (audioSize == "-" || size == 0L) {
            audioSize
        } else {
            "$audioSize kb"
        }
    }

    fun getCheckinTime(date: String?): String {
        return if (date == null || date == "") {
            convertTimeStampToStringFormat(null)
        } else {
            val checkinTime = DateTimeUtils.getDateFromString(date).time
            convertTimeStampToStringFormat(checkinTime)
        }
    }

    fun getFileSize(size: Long?): String {
        return if (size == null || size == 0L) {
            convertFileSizeToStringFormat(null)
        } else {
            convertFileSizeToStringFormat(size)
        }
    }
}
