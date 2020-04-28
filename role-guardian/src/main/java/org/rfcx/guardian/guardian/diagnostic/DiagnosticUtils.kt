package org.rfcx.guardian.guardian.diagnostic

object DiagnosticUtils {

    fun secondToTime(second: Int?): String {
        if (second != null) {
            var amountTime = ""
            val minutes = second / 60
            amountTime = if (minutes > 60L) {
                val hours = minutes / 60
                val min = minutes % 60
                if (min == 0) {
                    "$hours hours"
                } else {
                    "$hours hours and $min minutes"
                }
            } else {
                "$minutes minutes"
            }

            return amountTime
        } else {
            return "none"
        }
    }
}
