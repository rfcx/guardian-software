package org.rfcx.guardian.guardian.audio.capture

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.companion.CompanionSocketService
import org.rfcx.guardian.utility.misc.TimeUtils.isNowOutsideTimeRange
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs

class AudioScheduleService : Service() {

    companion object {
        const val SERVICE_NAME = "AudioSchedule"
    }

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioScheduleService")

    private var audioScheduleSvc: AudioScheduleSvc? = null
    private var runFlag = false

    private var app: RfcxGuardian? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        this.audioScheduleSvc = AudioScheduleSvc()
        this.app = application as RfcxGuardian
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.v(logTag, "Starting service: $logTag")
        this.runFlag = true
        app!!.rfcxSvc.setRunState(SERVICE_NAME, true)
        try {
            this.audioScheduleSvc?.start()
        } catch (e: IllegalThreadStateException) {
            RfcxLog.logExc(logTag, e)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runFlag = false
        app!!.rfcxSvc.setRunState(SERVICE_NAME, false)
    }

    private inner class AudioScheduleSvc : Thread("AudioScheduleService-AudioScheduleSvc") {
        override fun run() {
            val audioScheduleService = this@AudioScheduleService

            app = application as RfcxGuardian

            while (audioScheduleService.runFlag) {
                val offHour = app!!.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_CAPTURE_SCHEDULE_OFF_HOURS)
                if (!isNowOutsideTimeRange(offHour)) {
                    if (app!!.rfcxSvc.isRunning(AudioCaptureService.SERVICE_NAME)) {
                        Log.d(logTag, "Stopping AudioCaptureService due to off hours: $offHour")
                        app!!.rfcxSvc.stopService(AudioCaptureService.SERVICE_NAME)
                    }
                } else {
                    if (!app!!.rfcxSvc.isRunning(AudioCaptureService.SERVICE_NAME)) {
                        Log.d(logTag, "Starting AudioCaptureService due to off hours: $offHour")
                        app!!.rfcxSvc.triggerService(AudioCaptureService.SERVICE_NAME, false)
                    }
                }
                // To let AudioCaptureService take its time to start or stop itself
                sleep(1500)
            }

            app!!.rfcxSvc.setRunState(SERVICE_NAME, false)
            audioScheduleService.runFlag = false
            Log.v(logTag, "Stopping service: $logTag")
        }
    }
}
