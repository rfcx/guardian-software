package org.rfcx.guardian.guardian.activity

import android.text.format.DateFormat
import android.util.Log
import android.widget.TextView
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.utility.rfcx.RfcxLog
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.guardian.RfcxGuardian

class MainActivity : Activity() {
    var thread : Thread? = null

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

        preferencesButton.setOnClickListener { startActivity(Intent(this@MainActivity, PrefsActivity::class.java)) }

        changeRecordingState(app)
        changeButtonStateByRecordingState(app)
        getCheckinInformation(app)

        startButton.setOnClickListener {
            app.initializeRoleServices()
            app.recordingState = "true"
            changeRecordingState(app)
            changeButtonStateByRecordingState(app)
            getCheckinInformation(app)
        }

        stopButton.setOnClickListener {
            app.rfcxServiceHandler.stopAllServices()
            thread?.interrupt()
            app.recordingState = "false"
            changeRecordingState(app)
            changeButtonStateByRecordingState(app)
        }

        go_sync.setOnClickListener {
            val intent = Intent(this, SendDataActivity::class.java)
            startActivity(intent)
        }


    }

    private fun changeButtonStateByRecordingState(app: RfcxGuardian) {
        val state = app.sharedPrefs.getString("recordingState", null)
        when (state) {
            null -> {
                startButton.isClickable = true
                stopButton.isClickable = false

                startButton.alpha = 1.0f
                stopButton.alpha = 0.5f
            }
            "true" -> {
                startButton.isClickable = false
                stopButton.isClickable = true

                startButton.alpha = 0.5f
                stopButton.alpha = 1.0f
            }
            "false" -> {
                startButton.isClickable = true
                stopButton.isClickable = false

                startButton.alpha = 1.0f
                stopButton.alpha = 0.5f
            }
        }
    }

    private fun changeUiStatebyPrefs(app: RfcxGuardian) {
        if (app.rfcxPrefs.getPrefAsString("show_ui") == "false") {
            go_sync.visibility = View.INVISIBLE
            startButton.visibility = View.INVISIBLE
            stopButton.visibility = View.INVISIBLE
            status_view.visibility = View.INVISIBLE
        } else {
            go_sync.visibility = View.VISIBLE
            startButton.visibility = View.VISIBLE
            stopButton.visibility = View.VISIBLE
            status_view.visibility = View.VISIBLE
        }
    }

    private fun changeRecordingState(app: RfcxGuardian) {
        deviceIdText.text = app.rfcxDeviceGuid.deviceGuid
        if (app.recordingState == "true") {
            recordingStateText.text = getString(R.string.recording_state)
            recordingStateText.setTextColor(resources.getColor(R.color.primary))
        } else {
            recordingStateText.text = getString(R.string.notrecording_state)
            recordingStateText.setTextColor(resources.getColor(R.color.text_error))
        }
    }

    private fun getCheckinInformation(app: RfcxGuardian) {
        thread = object: Thread() {
            override fun run() {
                try{
                    Log.d("getInfoThread", "Started")
                    while(!isInterrupted){
                        runOnUiThread {
                            var lastestCheckIn = "-"
                            val checkinTime = app.sharedPrefs.getString("checkinTime", null)
                            if (checkinTime != null) {
                                lastestCheckIn = DateFormat.format(
                                    "yyyy-MM-dd'T'HH:mm:ss.mmm'Z'",
                                    java.lang.Long.parseLong(checkinTime)
                                ).toString()
                            }
                            checkInText.text = lastestCheckIn

                            var fileSize = "-"
                            val audioSize = app.sharedPrefs.getString("fileSize", null)
                            if (audioSize != null && audioSize != "0") {
                                fileSize = (Integer.parseInt(audioSize)/1000).toString()
                            }
                            if (fileSize == "-" && audioSize == "0") {
                                sizeText.text = fileSize
                            } else {
                                sizeText.text = "$fileSize kb"
                            }
                        }
                        sleep(5000)
                    }
                }catch (e: InterruptedException){
                    Log.d("getInfoThread", "Interrupted")
                }
            }
        }
        thread?.start()
    }

override fun onResume() {
    super.onResume()

    val app = application as RfcxGuardian
    changeUiStatebyPrefs(app)
    changeButtonStateByRecordingState(app)
    
    if(app.recordingState == "true"){
        getCheckinInformation(app)
    }
}

override fun onPause() {
    super.onPause()
    thread?.interrupt()
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
