package org.rfcx.guardian.admin.device.led

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxLog

class LedSequenceService: Service() {

    companion object {
        const val SERVICE_NAME = "LedSequence"
    }

    private val logTag = RfcxLog.generateLogTag(
        RfcxGuardian.APP_ROLE,
        "LedSequenceService"
    )

    private var app: RfcxGuardian? = null
    private var ledSequenceSvc: LedSequenceSvc? = null
    private var runFlag = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        ledSequenceSvc = LedSequenceSvc()
        app = application as RfcxGuardian
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.v(logTag, "Starting service: $logTag")
        this.runFlag = true
        app?.rfcxSvc?.setRunState(SERVICE_NAME, true)
        try {
            this.ledSequenceSvc?.start()
        } catch (e: IllegalThreadStateException) {
            RfcxLog.logExc(logTag, e)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runFlag = false
        app?.rfcxSvc?.setRunState(SERVICE_NAME, false)
        this.ledSequenceSvc?.interrupt()
        this.ledSequenceSvc = null
    }

    private inner class LedSequenceSvc: Thread("LedSequenceService-LedSequenceSvc") {
        override fun run() {
            val ledSequenceInstance: LedSequenceService = this@LedSequenceService
            app = application as RfcxGuardian

            while (ledSequenceInstance.runFlag) {
                app?.ledSequenceUtils?.ledOff()
                if (app?.isGuardianRegistered == true) {
                    sleep(10000)
                } else {
                    sleep(1000)
                }
                app?.ledSequenceUtils?.ledOn()
            }

            app?.rfcxSvc?.setRunState(SERVICE_NAME, false)
            ledSequenceInstance.runFlag = false
            Log.v(ledSequenceInstance.logTag, "Stopping service: " + ledSequenceInstance.logTag)
        }
    }
}
