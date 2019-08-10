package org.rfcx.guardian.guardian.utils

import java.lang.Long

class CheckInInformationUtils {

    fun convertTimeStampToStringFormat(timestamp: String?): String{
        if (timestamp != null) {
            var latestCheckInStr = ""
            val latestCheckIn = System.currentTimeMillis() - Long.parseLong(timestamp)
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
        }else{
            return "none"
        }
    }

    fun convertFileSizeToStringFormat(fileSize: String?): String{
        var audioSize = "-"
        if (fileSize != null && fileSize != "0") {
            audioSize = (Integer.parseInt(fileSize) / 1000).toString()
        }

        return if (audioSize == "-" && fileSize == "0") {
            audioSize
        }else{
            "$audioSize kb"
        }
    }
}