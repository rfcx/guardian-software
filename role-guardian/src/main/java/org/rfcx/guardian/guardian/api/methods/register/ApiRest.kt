package org.rfcx.guardian.guardian.api.methods.register

import android.content.Context
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxPrefs

class ApiRest {
    companion object {
        fun baseUrl(context: Context): String {
            val prefs = (context.getApplicationContext() as RfcxGuardian).rfcxPrefs
            val protocol = prefs.getPrefAsString(RfcxPrefs.Pref.API_REST_PROTOCOL)
            val host = prefs.getPrefAsString(RfcxPrefs.Pref.API_REST_HOST)
            if (protocol != null && host != null) {
                return "${protocol}://${host}/"
            }
            return "https://api.rfcx.org/"
        }
    }
}
