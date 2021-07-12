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

//        val prefsAdminEnableWifi = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_WIFI_FUNCTION).equalsIgnoreCase("")

        if (prefsAdminEnableWifiSocket/* && !prefsAdminEnableWifi*/) {
            Log.e( logTag, "WiFi Socket Server could not be enabled because 'admin_enable_wifi_hotspot' is disabled")
        }
        try {
            if (prefsAdminEnableWifiSocket/* && prefsAdminEnableWifi*/) {
                if (!SocketManager.isRunning) {
                    Log.d(logTag, "Starting WifiCommunication service")
                    SocketManager.stopServerSocket()
                    SocketManager.startServerSocket(applicationContext)
                }
            } else {
                if (SocketManager.isRunning) {
                    Log.d(logTag, "Stopping WifiCommunication service")
                    SocketManager.stopServerSocket()
                }
            }
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        }
    }
}