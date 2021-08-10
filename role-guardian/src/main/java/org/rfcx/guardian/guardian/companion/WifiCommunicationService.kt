package org.rfcx.guardian.guardian.companion

import android.app.IntentService
import android.content.Intent
import android.util.Log
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs
import org.rfcx.guardian.utility.rfcx.RfcxSvc

class WifiCommunicationService : IntentService("WifiCommunication") {

    public val SERVICE_NAME = "WifiCommunication"

    private val logTag = RfcxLog.generateLogTag(
        RfcxGuardian.APP_ROLE,
        "WifiCommunicationService"
    )

    override fun onHandleIntent(p0: Intent?) {
        val intent = Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME))
        sendBroadcast( intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME))

        val app = application as RfcxGuardian

        app.rfcxSvc.reportAsActive(SERVICE_NAME)

        val prefsAdminEnableWifiSocket = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_WIFI_SOCKET)
        val prefsAdminWifiFunction = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION)
        val isWifiEnabled = prefsAdminWifiFunction.equals("hotspot") || prefsAdminWifiFunction.equals("client")

        if (prefsAdminEnableWifiSocket && !isWifiEnabled) {
            Log.e( logTag, "WiFi Socket Server could not be enabled because 'admin_wifi_function' is set to off.")
        }
        try {
            if (prefsAdminEnableWifiSocket && isWifiEnabled) {
                if (!app.apiSocketUtils.isServerRunning) {
                    Log.d(logTag, "Starting WifiCommunication service")
                    app.apiSocketUtils.stopServer();
                    app.apiSocketUtils.startServer();
                }
            } else {
                if (app.apiSocketUtils.isServerRunning) {
                    Log.d(logTag, "Stopping WifiCommunication service")
                    app.apiSocketUtils.stopServer()
                }
            }
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        }
    }
}
