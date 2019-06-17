package org.rfcx.guardian.guardian.activity

import android.os.AsyncTask
import android.text.format.DateFormat
import android.util.Log
import android.widget.TextView
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureService
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeJobService
import org.rfcx.guardian.utility.rfcx.RfcxLog
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.guardian.RfcxGuardian

import java.net.URL

class MainActivity : Activity() {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val app = application as RfcxGuardian

        when (item.itemId) {

            R.id.menu_prefs -> startActivity(Intent(this, PrefsActivity::class.java))

            R.id.menu_reboot -> app.deviceControlUtils.runOrTriggerDeviceControl(
                "reboot",
                app.applicationContext.contentResolver
            )

            R.id.menu_relaunch -> app.deviceControlUtils.runOrTriggerDeviceControl(
                "relaunch",
                app.applicationContext.contentResolver
            )

            R.id.menu_screenshot -> app.deviceControlUtils.runOrTriggerDeviceControl(
                "screenshot",
                app.applicationContext.contentResolver
            )

            R.id.menu_logcat -> app.deviceControlUtils.runOrTriggerDeviceControl(
                "logcat",
                app.applicationContext.contentResolver
            )

            R.id.menu_sntp -> app.deviceControlUtils.runOrTriggerDeviceControl(
                "datetime_sntp_sync",
                app.applicationContext.contentResolver
            )

            R.id.menu_purge_checkins -> app.apiCheckInUtils.purgeAllCheckIns()
        }

        return true
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val app = application as RfcxGuardian

        val preferences = findViewById<View>(R.id.preferencesButton) as Button
        preferences.setOnClickListener { startActivity(Intent(this@MainActivity, PrefsActivity::class.java)) }

        val start = findViewById<View>(R.id.startButton) as Button
        start.setOnClickListener {
            app.initializeRoleServices()
            //getCheckinInformation()
        }

        val stop = findViewById<View>(R.id.stopButton) as Button
        stop.setOnClickListener { app.rfcxServiceHandler.stopAllServices() }

        go_sync.setOnClickListener {
            val intent = Intent(this, SendDataActivity::class.java)
            startActivity(intent)
        }


    }

    private fun getCheckinInformation() {
        val checkinText = findViewById<View>(R.id.checkInText) as TextView
        val sizeText = findViewById<View>(R.id.sizeText) as TextView
        val app = application as RfcxGuardian
        val audioEncodeJobService = AudioEncodeJobService()
        object : Thread() {
            override fun run() {
                while (true) {
                    try {
                        runOnUiThread {
                            var lastestCheckIn = ""
                            if (app.apiCheckInUtils.lastTimeCheckIn != null) {
                                lastestCheckIn = DateFormat.format(
                                    "yyyy-MM-dd'T'HH:mm:ss.mmm'Z'",
                                    java.lang.Long.parseLong(app.apiCheckInUtils.lastTimeCheckIn)
                                ).toString()
                            }
                            checkinText.text = lastestCheckIn

                            var fileSize = ""
                            if (audioEncodeJobService.encodedFileSize != null) {
                                fileSize = (Integer.parseInt(audioEncodeJobService.encodedFileSize) / 1000).toString()
                            }
                            sizeText.text = fileSize + "kb"
                        }
                        sleep(50000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        (application as RfcxGuardian).appPause()
    }

    companion object {
        private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, MainActivity::class.java)
        fun startActivity(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }

//        private const val REQUEST_CODE_REPORT = 201
//        private const val REQUEST_CODE_GOOGLE_AVAILABILITY = 100
//        const val INTENT_FILTER_MESSAGE_BROADCAST = "${BuildConfig.APPLICATION_ID}.MESSAGE_RECEIVE"
//        const val CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
//        private const val LIMIT_PER_PAGE = 12
    }


}
