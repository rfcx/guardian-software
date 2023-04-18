package org.rfcx.guardian.utility.rfcx;

import android.content.Context;

public class RfcxMode {
    public static boolean isOfflineMode(RfcxPrefs prefs) {
        return prefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_PROTOCOL).equalsIgnoreCase("off") &&
                prefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_CLASSIFY) == false &&
                prefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CHECKIN_PUBLISH) == false &&
                prefs.getPrefAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).equalsIgnoreCase("") &&
                prefs.getPrefAsString(RfcxPrefs.Pref.API_PING_SCHEDULE_OFF_HOURS).equalsIgnoreCase("00:00-23:59");
    }
}
