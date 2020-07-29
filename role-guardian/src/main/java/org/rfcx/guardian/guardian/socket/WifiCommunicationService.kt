package org.rfcx.guardian.guardian.socket

import android.app.IntentService
import android.content.Intent
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.service.RfcxServiceHandler

class WifiCommunicationService : IntentService("WifiCommunication") {

    private val SERVICE_NAME = "WifiCommunication"

    private val logTag = RfcxLog.generateLogTag(
        RfcxGuardian.APP_ROLE,
        "WifiCommunicationService"
    )

    override fun onHandleIntent(p0: Intent?) {
        val intent =
            Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME))
        sendBroadcast(
            intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME))

        val app = application as RfcxGuardian
        val state = app.rfcxPrefs.getPrefAsBoolean("admin_enable_wifi_socket")
        try {
            if (state) {
                SocketManager.stopServerSocket()
                SocketManager.startServerSocket(applicationContext)
            } else {
                SocketManager.stopServerSocket()
            }
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        }
    }
}
