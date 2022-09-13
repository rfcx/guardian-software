package org.rfcx.guardian.admin.device.i2c

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.utility.misc.TimeUtils.isNowOutsideTimeRange
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs

class DeviceSensorService : Service() {

    companion object {
        const val SERVICE_NAME = "DeviceSensor"
    }

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DeviceSensorService")

    private var deviceSensorSvc: DeviceSensorSvc? = null
    private var runFlag = false

    private var app: RfcxGuardian? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        this.deviceSensorSvc = DeviceSensorSvc()
        this.app = application as RfcxGuardian
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.v(logTag, "Starting service: $logTag")
        this.runFlag = true
        app!!.rfcxSvc.setRunState(SERVICE_NAME, true)
        try {
            this.deviceSensorSvc?.start()
        } catch (e: IllegalThreadStateException) {
            RfcxLog.logExc(logTag, e)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runFlag = false
        app!!.rfcxSvc.setRunState(SERVICE_NAME, false)
        this.deviceSensorSvc?.interrupt()
        this.deviceSensorSvc = null
    }

    private inner class DeviceSensorSvc() : Thread("DeviceSensorService-DeviceSensorSvc") {

        override fun run() {
            val deviceSensorService = this@DeviceSensorService

            app = application as RfcxGuardian

            while (deviceSensorService.runFlag) {
                if (!isNowOutsideTimeRange(app!!.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_DIAGNOSTIC_OFF_HOURS))) {
                    continue
                }

                try {
                    if (app!!.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_SENSOR_BME688) && app!!.sentryBME688Utils.isChipAccessibleByI2c()) {
                        Log.d(logTag, "Saving BME688 values to database")
                        app!!.sentryBME688Utils.saveBME688ValuesToDatabase(app!!.sentryBME688Utils.getBME688Values())
                    }

                    if (app!!.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_SENSOR_INFINEON) && app!!.sentryInfineonUtils.isChipAccessibleByI2c()) {
                        Log.d(logTag, "Saving Infineon values to database")
                        app!!.sentryInfineonUtils.saveInfineonValuesToDatabase(app!!.sentryInfineonUtils.getInfineonValues())
                    }

                    sleep(300000)
                } catch (e: InterruptedException) {
                    deviceSensorService.runFlag = false
                    app!!.rfcxSvc.setRunState(SERVICE_NAME, false)
                    RfcxLog.logExc(logTag, e)
                }
            }
        }
    }
}
