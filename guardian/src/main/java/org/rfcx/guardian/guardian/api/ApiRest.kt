package org.rfcx.guardian.guardian.api

import android.content.Context
import org.rfcx.guardian.guardian.RfcxGuardian

class ApiRest {
    companion object {
        fun baseUrl(context: Context): String {
            val prefs = (context.getApplicationContext() as RfcxGuardian).rfcxPrefs
            val protocol = prefs.getPrefAsString("api_rest_protocol")
            val host = prefs.getPrefAsString("api_rest_host")
            if (protocol != null && host != null) {
                return "${protocol}://${host}/"
            }
            return "https://api.rfcx.org/"
        }
    }
}
