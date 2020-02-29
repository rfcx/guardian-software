package org.rfcx.guardian.admin.device.android.verify

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.misc.ShellCommands
import org.rfcx.guardian.utility.rfcx.RfcxLog

class ConnectivityVerifyService : Service() {

    private val logTag =
        RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ConnectivityVerifyService::class.java)

    private val SERVICE_NAME = "ConnectivityVerify"

    private lateinit var app: RfcxGuardian

    private var runFlag = false
    private lateinit var connectivityVerifyJob: ConnectivityVerifyJob

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        connectivityVerifyJob = ConnectivityVerifyJob()
        app = application as RfcxGuardian
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        Log.v(logTag, "Starting service: $logTag")
        this.runFlag = true
        app.rfcxServiceHandler.setRunState(SERVICE_NAME, true)
        try {
            connectivityVerifyJob.start()
        } catch (e: IllegalThreadStateException) {
            RfcxLog.logExc(logTag, e)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        this.runFlag = false
        app.rfcxServiceHandler.setRunState(SERVICE_NAME, false)
        connectivityVerifyJob.interrupt()
    }

    private inner class ConnectivityVerifyJob :
        Thread("ConnectivityVerifyService-ConnectivityVerifyJob") {

        val minutesLimit = 15
        val countLimit = (minutesLimit / 1.5).toInt()

        override fun run() {
            super.run()

            val connectivityVerifyJobInstance = ConnectivityVerifyService()
            try {
                app.rfcxServiceHandler.reportAsActive(SERVICE_NAME)

                if (!app.deviceConnectivity.isConnected) {
                    app.connectivityTimeoutCounter += 1
                    Log.d(
                        logTag,
                        "This device connection was down for ${app.connectivityTimeoutCounter * 90} seconds. There are more ${(minutesLimit*60) - (app.connectivityTimeoutCounter * 90)} second before force reboot"
                    )
                } else {
                    app.connectivityTimeoutCounter = 0
                }
                if (app.connectivityTimeoutCounter == countLimit) {
                    ShellCommands.triggerRebootAsRoot(app.applicationContext)
                }

            } catch (e: Exception) {
                RfcxLog.logExc(logTag, e)
            } finally {
                connectivityVerifyJobInstance.runFlag = false
                app.rfcxServiceHandler.setRunState(SERVICE_NAME, false)
                app.rfcxServiceHandler.stopService(SERVICE_NAME)
            }

        }
    }

}
