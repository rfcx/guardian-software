package org.rfcx.guardian.admin.device.android.wifi

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxLog

class WifiCommunicationService : Service() {

    private val SERVICE_NAME = "WifiCommunication"

    private val logTag = RfcxLog.generateLogTag(
        RfcxGuardian.APP_ROLE,
        "WifiCommunicationService"
    )

    private var app: RfcxGuardian? = null

    private var runFlag = false

    private var wifiCommunication: WifiCommunication? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        app = application as RfcxGuardian
        wifiCommunication = WifiCommunication()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(logTag, "Starting service: $logTag")
        runFlag = true
        app!!.rfcxServiceHandler.setRunState(SERVICE_NAME, true)
        try {
            wifiCommunication!!.start()
        } catch (e: IllegalThreadStateException) {
            RfcxLog.logExc(logTag, e)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runFlag = false
        app!!.rfcxServiceHandler.setRunState(SERVICE_NAME, false)
        wifiCommunication!!.interrupt()
        wifiCommunication = null
    }

    inner class WifiCommunication : Thread("WifiCommunicationService-WifiCommunication") {

        override fun run() {
            super.run()

            try {
                val state = app!!.rfcxPrefs.getPrefAsBoolean("admin_enable_wifi_socket")
                if (state) {
                    WifiCommunicationUtils.startServerSocket(applicationContext)
                } else {
                    WifiCommunicationUtils.stopServerSocket()
                }
            } catch (e: Exception) {
                RfcxLog.logExc(logTag, e)
            } finally {
                runFlag = false
                app!!.rfcxServiceHandler.setRunState(SERVICE_NAME, false)
                app!!.rfcxServiceHandler.stopService(SERVICE_NAME)
            }
        }
    }

}
