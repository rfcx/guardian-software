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
import android.support.v7.app.AppCompatActivity
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
import org.rfcx.guardian.guardian.manager.PreferenceManager
import org.rfcx.guardian.guardian.manager.getTokenID
import org.rfcx.guardian.guardian.manager.getUserNickname
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Integer.parseInt
import java.lang.Long.parseLong

class MainActivity : AppCompatActivity() {
    var thread: Thread? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

//        val app = application as RfcxGuardian

        when (item.itemId) {

            R.id.menu_prefs -> startActivity(Intent(this, PrefsActivity::class.java))
            R.id.menu_sync -> startActivity(Intent(this, SendDataActivity::class.java))
//            R.id.menu_reboot -> app.deviceControlUtils.runOrTriggerDeviceControl(
//                "reboot",
//                app.applicationContext.contentResolver
//            )
//
//            R.id.menu_relaunch -> app.deviceControlUtils.runOrTriggerDeviceControl(
//                "relaunch",
//                app.applicationContext.contentResolver
//            )
//
//            R.id.menu_screenshot -> app.deviceControlUtils.runOrTriggerDeviceControl(
//                "screenshot",
//                app.applicationContext.contentResolver
//            )
//
//            R.id.menu_logcat -> app.deviceControlUtils.runOrTriggerDeviceControl(
//                "logcat",
//                app.applicationContext.contentResolver
//            )
//
//            R.id.menu_sntp -> app.deviceControlUtils.runOrTriggerDeviceControl(
//                "datetime_sntp_sync",
//                app.applicationContext.contentResolver
//            )
//
//            R.id.menu_purge_checkins -> app.apiCheckInUtils.purgeAllCheckIns()
        }

        return true
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)
        val toolbar = supportActionBar
        toolbar?.title = "Guardian"
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

        val loginStatus = intent.getStringExtra("LOGIN_STATUS")

        if (isGuidExisted()) {
            registerButton.isEnabled = false
        }

        changeRecordingState(app)
        changeButtonStateByRecordingState(app)
        getCheckinInformation(app)
        changeLoginState()
        changeRegisterState()
//        createRegisterFile(app)

        startButton.setOnClickListener {
            if(!isGuidExisted()){
                Toast.makeText(this, "Please register this guardian first", Toast.LENGTH_LONG).show()
            }else {
                app.initializeRoleServices()
                app.recordingState = "true"
                changeRecordingState(app)
                changeButtonStateByRecordingState(app)
                getCheckinInformation(app)
            }
        }

        stopButton.setOnClickListener {
            app.rfcxServiceHandler.stopAllServices()
            thread?.interrupt()
            app.recordingState = "false"
            changeRecordingState(app)
            changeButtonStateByRecordingState(app)
        }

        registerButton.setOnClickListener {
            if (isNetworkAvailable(this)) {
                if (this.getTokenID() == null) {
                    Toast.makeText(this, "Please login first.", Toast.LENGTH_LONG).show()
                } else {
                    if (!isGuidExisted()) {
                        register_warning.visibility = View.INVISIBLE
                        record_group.visibility = View.INVISIBLE
                        start_stop_group.visibility = View.INVISIBLE
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
                                    register_warning.visibility = View.INVISIBLE
                                    registerButton.visibility = View.INVISIBLE
                                    registerInfo.visibility = View.VISIBLE
                                    record_group.visibility = View.VISIBLE
                                    start_stop_group.visibility = View.VISIBLE
                                    registerProgress.visibility = View.INVISIBLE
                                }

                                override fun onFailed(t: Throwable?, message: String?) {
                                    registerButton.visibility = View.VISIBLE
                                    registerInfo.visibility = View.INVISIBLE
                                    record_group.visibility = View.INVISIBLE
                                    start_stop_group.visibility = View.INVISIBLE
                                    registerProgress.visibility = View.INVISIBLE
                                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                                    Log.d("register_failed", t.toString())
                                }

                            })
                    } else {
                        registerButton.visibility = View.INVISIBLE
                        registerInfo.visibility = View.VISIBLE
                    }
                }
            } else {
                Toast.makeText(this, "There is not internet connection. Please turn it on.", Toast.LENGTH_LONG).show()
            }
        }

        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            PreferenceManager.getInstance(this).clear()
            finish()
            startActivity(intent)
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
            register_group.visibility = View.INVISIBLE
            record_group.visibility = View.INVISIBLE
            start_stop_group.visibility = View.INVISIBLE
            login_group.visibility = View.INVISIBLE
        } else {
            register_group.visibility = View.VISIBLE
            record_group.visibility = View.VISIBLE
            start_stop_group.visibility = View.VISIBLE
            login_group.visibility = View.VISIBLE
        }
    }

    private fun changeRecordingState(app: RfcxGuardian) {
        if (isGuidExisted()) {
            Log.d("Guid", "existed")
            deviceIdText.text = readRegisterFile()
        } else {
            Log.d("Guid", "not existed")
            record_image.setImageResource(R.drawable.not_registered_sign)
            recordingStateText.text = "NOT REGISTERED"
        }
        if (app.recordingState == "true" && isGuidExisted()) {
            recordingStateText.text = getString(R.string.recording_state)
            recordingStateText.setTextColor(resources.getColor(R.color.text_error))
            record_image.setImageResource(R.drawable.recorded_sign3)
        } else if(app.recordingState == "false" && isGuidExisted()) {
            recordingStateText.text = getString(R.string.notrecording_state)
            recordingStateText.setTextColor(resources.getColor(R.color.text_black))
            record_image.setImageResource(R.drawable.not_record_sign3)
        }
    }

    private fun changeLoginState() {
        if (this.getTokenID() == null) {
            loginButton.visibility = View.VISIBLE
            loginInfo.visibility = View.INVISIBLE
        } else {
            loginButton.visibility = View.INVISIBLE
            loginInfo.visibility = View.VISIBLE
            userName.text = this.getUserNickname()
        }
    }
    private fun changeRegisterState() {
        if(isGuidExisted()){
            record_group.visibility = View.VISIBLE
            start_stop_group.visibility = View.VISIBLE
            registerButton.visibility = View.INVISIBLE
            registerInfo.visibility = View.VISIBLE
            register_warning.visibility = View.INVISIBLE
            deviceIdText.text = readRegisterFile()
        }else{
            record_group.visibility = View.INVISIBLE
            start_stop_group.visibility = View.INVISIBLE
            registerButton.visibility = View.VISIBLE
            registerInfo.visibility = View.INVISIBLE
            register_warning.visibility = View.VISIBLE

        }
    }

    private fun getCheckinInformation(app: RfcxGuardian) {
        thread = object : Thread() {
            override fun run() {
                try {
                    Log.d("getInfoThread", "Started")
                    while (!isInterrupted) {
                        runOnUiThread {
                            var lastestCheckIn = 0.toLong()
                            var lastestCheckinStr = "none"
                            val checkinTime = app.sharedPrefs.getString("checkinTime", null)
                            if (checkinTime != null) {
                                lastestCheckIn = System.currentTimeMillis() - parseLong(checkinTime)
                                val minutes = lastestCheckIn/60000
                                if(minutes > 60L){
                                    val hours = minutes/60
                                    val min = minutes%60
                                    if(min == 0L){
                                        lastestCheckinStr = "$hours hours"
                                    }else{
                                        lastestCheckinStr = "$hours hours and $min minutes ago"
                                    }
                                }else{
                                    lastestCheckinStr = "$minutes minutes ago"
                                }
                            }
                            checkInText.text = lastestCheckinStr

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
        changeLoginState()
        changeRegisterState()

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
//        val app = application as RfcxGuardian
//        app.rfcxServiceHandler.stopAllServices()
//        app.recordingState = "false"
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
