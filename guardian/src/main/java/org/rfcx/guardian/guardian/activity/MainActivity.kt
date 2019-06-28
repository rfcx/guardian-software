package org.rfcx.guardian.guardian.activity

import android.text.format.DateFormat
import android.util.Log
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.utility.rfcx.RfcxLog
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.api.RegisterApi
import org.rfcx.guardian.guardian.entity.RegisterRequest
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : Activity() {
    var thread: Thread? = null

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

//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
//            try {
//                ProviderInstaller.installIfNeeded(this)
//            } catch (e: GooglePlayServicesRepairableException) {
//                Log.d("google", "please install gg service")
//                GoogleApiAvailability.getInstance()
//                    .showErrorNotification(this, e.connectionStatusCode)
//            } catch (e: GooglePlayServicesNotAvailableException) {
//                Log.d("google", "you cannot use it")
//            }
//        }

        val app = application as RfcxGuardian

        preferencesButton.setOnClickListener { startActivity(Intent(this@MainActivity, PrefsActivity::class.java)) }

        val loginStatus = intent.getStringExtra("LOGIN_STATUS")

        if(loginStatus == "skip"){
            registerButton.isEnabled = false
        }
        if(isGuidExisted()){
            registerButton.isEnabled = false
        }

        if(app.recordingState == "false"){
            app.initializeRoleServices()
            app.recordingState = "true"
        }
        changeRecordingState(app)
        changeButtonStateByRecordingState(app)
        getCheckinInformation(app)
//        createRegisterFile(app)

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

        registerButton.setOnClickListener {
            if (isNetworkAvailable(this)) {
                if (!isGuidExisted()) {
                    guidLayout.visibility = View.INVISIBLE
                    status_view.visibility = View.INVISIBLE
                    startButton.visibility = View.INVISIBLE
                    stopButton.visibility = View.INVISIBLE
                    registerProgress.visibility = View.VISIBLE

                    val guid = app.rfcxDeviceGuid.deviceGuid
                    val token = app.rfcxDeviceGuid.deviceToken
                    Log.d("GuidInfo", app.rfcxDeviceGuid.deviceGuid)
                    Log.d("GuidInfo", app.rfcxDeviceGuid.deviceToken)
                    RegisterApi().registerGuardian(
                        this,
                        RegisterRequest(guid, token),
                        object : RegisterApi.RegisterCallback {
                            override fun onSuccess() {
                                createRegisterFile(app)
                                changeRecordingState(app)
                                deviceIdText.text = readRegisterFile()
                                guidLayout.visibility = View.VISIBLE
                                status_view.visibility = View.VISIBLE
                                startButton.visibility = View.VISIBLE
                                stopButton.visibility = View.VISIBLE
                                registerProgress.visibility = View.INVISIBLE
                            }

                            override fun onFailed(t: Throwable?, message: String?) {
                                guidLayout.visibility = View.VISIBLE
                                status_view.visibility = View.VISIBLE
                                startButton.visibility = View.VISIBLE
                                stopButton.visibility = View.VISIBLE
                                registerProgress.visibility = View.INVISIBLE
                                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                                Log.d("register_failed", t.toString())
                            }

                        })
                } else {
                    registerButton.isEnabled = false
                }
            } else {
                Toast.makeText(this, "There is not internet connection. Please turn it on.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun changeButtonStateByRecordingState(app: RfcxGuardian) {
        val state = app.sharedPrefs.getString("recordingState", null)
        when (state) {
            null -> {
                startButton.isEnabled = true
                stopButton.isEnabled = false
            }
            "true" -> {
                startButton.isEnabled = false
                stopButton.isEnabled = true
            }
            "false" -> {
                startButton.isEnabled = true
                stopButton.isEnabled = false
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
        if (isGuidExisted()) {
            Log.d("Guid","existed")
            deviceIdText.text = readRegisterFile()
        } else {
            Log.d("Guid","not existed")
            deviceIdText.text = "not registered"
        }
        if (app.recordingState == "true") {
            recordingStateText.text = getString(R.string.recording_state)
            recordingStateText.setTextColor(resources.getColor(R.color.primary))
        } else {
            recordingStateText.text = getString(R.string.notrecording_state)
            recordingStateText.setTextColor(resources.getColor(R.color.text_error))
        }
    }

    private fun getCheckinInformation(app: RfcxGuardian) {
        thread = object : Thread() {
            override fun run() {
                try {
                    Log.d("getInfoThread", "Started")
                    while (!isInterrupted) {
                        runOnUiThread {
                            var lastestCheckIn = "none"
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
                                fileSize = (Integer.parseInt(audioSize) / 1000).toString()
                            }
                            if (fileSize == "-" && audioSize == "0") {
                                sizeText.text = fileSize
                            } else {
                                sizeText.text = "$fileSize kb"
                            }
                        }
                        sleep(5000)
                    }
                } catch (e: InterruptedException) {
                    Log.d("getInfoThread", "Interrupted")
                }
            }
        }
        thread?.start()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun isGuidExisted(): Boolean {
        val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString()
        val txtFile = File(directoryPath + "/register.txt")
        return txtFile.exists()
    }

    private fun createRegisterFile(app: RfcxGuardian) {
        val externalPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        externalPath.mkdir()
        val file = File(externalPath, "register.txt")
        FileOutputStream(file).use {
            it.write(app.rfcxDeviceGuid.deviceGuid.toByteArray())
        }
    }

    private fun readRegisterFile(): String {
        val externalPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val file = File(externalPath, "register.txt")
        return FileInputStream(file).bufferedReader().use { it.readText() }
    }

    override fun onResume() {
        super.onResume()

        val app = application as RfcxGuardian
        changeUiStatebyPrefs(app)
        changeRecordingState(app)
        changeButtonStateByRecordingState(app)

        if (app.recordingState == "true") {
            getCheckinInformation(app)
        }
    }

    override fun onPause() {
        super.onPause()
        thread?.interrupt()
    }

    override fun onDestroy() {
        super.onDestroy()
        val app = application as RfcxGuardian
        app.rfcxServiceHandler.stopAllServices()
        app.recordingState = "false"
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
