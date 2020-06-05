package org.rfcx.guardian.admin.device.android.ssh

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxLog

class SSHServerControlService : Service() {

    private val SERVICE_NAME = "SSHServerControl"

    private val logTag = RfcxLog.generateLogTag(
        RfcxGuardian.APP_ROLE,
        "SSHServerControlService"
    )

    private var app: RfcxGuardian? = null

    private var runFlag = false

    private var sshServerControl: SSHServerControl? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        sshServerControl = SSHServerControl()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(logTag, "Starting service: $logTag")
        runFlag = true
        app!!.rfcxServiceHandler.setRunState(SERVICE_NAME, true)
        try {
            sshServerControl!!.start()
        } catch (e: IllegalThreadStateException) {
            RfcxLog.logExc(logTag, e)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runFlag = false
        app!!.rfcxServiceHandler.setRunState(SERVICE_NAME, false)
        sshServerControl!!.interrupt()
        sshServerControl = null
    }

    inner class SSHServerControl : Thread("SSHServerControlService-SSHServerControl") {

        override fun run() {
            super.run()

            app = application as RfcxGuardian
            try {
                val state = app!!.rfcxPrefs.getPrefAsBoolean("admin_enable_ssh_server")
                if (state) {
                    SSHServerUtils.startServer(applicationContext)
                } else {
                    SSHServerUtils.stopServer(applicationContext)
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