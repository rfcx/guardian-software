package org.rfcx.guardian.guardian.activity

import android.util.Log
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.utility.rfcx.RfcxLog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.security.ProviderInstaller
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.api.RegisterApi
import org.rfcx.guardian.guardian.entity.RegisterRequest
import org.rfcx.guardian.guardian.manager.PreferenceManager
import org.rfcx.guardian.guardian.manager.getTokenID
import org.rfcx.guardian.guardian.manager.getUserNickname
import org.rfcx.guardian.guardian.utils.CheckInInformationUtils
import org.rfcx.guardian.utility.datetime.DateTimeUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    var getInfoThread: Thread? = null

    override fun onResume() {
        super.onResume()

        val app = application as RfcxGuardian
        setVisibilityByPrefs(app)
        setUIByLogin()
        setUIByRegister(app)
        registerButton.isEnabled = !isGuidExisted()

        Handler().postDelayed({
            setUIByRecordingState(app)
            setBtnEnableByRecordingState(app)
            if (app.recordingState) {
                getCheckinInformation(app)
            }
        },500)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_prefs -> startActivity(Intent(this, PrefsActivity::class.java))
        }

        return true
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)
        toolBarInit()

        val app = application as RfcxGuardian

        startButton.setOnClickListener {
            if (!isGuidExisted()) {
                Toast.makeText(this, "Please register this guardian first", Toast.LENGTH_LONG)
                    .show()
            } else if(!app.isLocationEnabled){
                Toast.makeText(this, "Please enable gps location", Toast.LENGTH_LONG)
                    .show()
            } else {
                app.initializeRoleServices()
                setUIFromBtnClicked("start")
                getCheckinInformation(app)
            }
        }

        stopButton.setOnClickListener {
            app.rfcxServiceHandler.stopAllServices()
            getInfoThread?.interrupt()
            setUIFromBtnClicked("stop")
        }

        registerButton.setOnClickListener {
            if (isNetworkAvailable(this)) {
                if (this.getTokenID() != null) {
                    if (!isGuidExisted()) {
                        setVisibilityBeforeRegister()
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
                                    setUIByRecordingState(app)
                                    setUIByRegister(app)
                                    setVisibilityRegisterSuccess()
                                    deviceIdText.text = readRegisterFile()
                                }

                                override fun onFailed(t: Throwable?, message: String?) {
                                    setVisibilityRegisterFailed()
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
            val intent = Intent(this, LoginWebViewActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            PreferenceManager.getInstance(this).clear()
            finish()
            startActivity(intent)
        }

        i2cSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked){
                app.setPref("checkin_with_i2c_battery","true")
            }else{
                app.setPref("checkin_with_i2c_battery","false")
            }
        }
    }

    private fun toolBarInit() {
        val toolbar = supportActionBar
        toolbar?.title = "Guardian"
    }

    private fun setBtnEnableByRecordingState(app: RfcxGuardian) {
        when (app.recordingState) {
            true -> {
                startButton.isEnabled = false
                stopButton.isEnabled = true
            }
            false -> {
                startButton.isEnabled = true
                stopButton.isEnabled = false
            }
        }
    }

    private fun setUIFromBtnClicked(button: String) {
        if (button == "start") {
            startButton.isEnabled = false
            stopButton.isEnabled = true
            recordingStateText.text = getString(R.string.recording_state)
            recordingStateText.setTextColor(ContextCompat.getColor(this, R.color.text_error))
            record_image.setImageResource(R.drawable.recorded_sign3)
        } else {
            startButton.isEnabled = true
            stopButton.isEnabled = false
            recordingStateText.text = getString(R.string.notrecording_state)
            recordingStateText.setTextColor(ContextCompat.getColor(this, R.color.text_black))
            record_image.setImageResource(R.drawable.not_record_sign3)
        }
    }

    private fun setVisibilityByPrefs(app: RfcxGuardian) {
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

    private fun setUIByRecordingState(app: RfcxGuardian) {
            if (isGuidExisted()) {
                Log.d("Guid", "existed")
                deviceIdText.text = readRegisterFile()
                if (app.recordingState) {
                    recordingStateText.text = getString(R.string.recording_state)
                    recordingStateText.setTextColor(ContextCompat.getColor(this, R.color.text_error))
                    record_image.setImageResource(R.drawable.recorded_sign3)
                } else {
                    recordingStateText.text = getString(R.string.notrecording_state)
                    recordingStateText.setTextColor(ContextCompat.getColor(this, R.color.text_black))
                    record_image.setImageResource(R.drawable.not_record_sign3)
                }
            } else {
                Log.d("Guid", "not existed")
                record_image.setImageResource(R.drawable.not_registered_sign)
                recordingStateText.text = "NOT REGISTERED"
            }
    }

    private fun setUIByLogin() {
        if (this.getTokenID() == null) {
            loginButton.visibility = View.VISIBLE
            loginInfo.visibility = View.INVISIBLE
            registerButton.isEnabled = false
            registerButton.alpha = 0.5f
        } else {
            loginButton.visibility = View.INVISIBLE
            loginInfo.visibility = View.VISIBLE
            registerButton.isEnabled = true
            registerButton.alpha = 1.0f
            userName.text = this.getUserNickname()
        }
    }

    private fun setUIByRegister(app: RfcxGuardian) {
        if (isGuidExisted()) {
            record_group.visibility = View.VISIBLE
            start_stop_group.visibility = View.VISIBLE
            registerButton.visibility = View.INVISIBLE
            registerInfo.visibility = View.VISIBLE
            i2cSwitch.visibility = View.VISIBLE
            permissionInfoLayout.visibility = View.VISIBLE
            deviceIdText.text = readRegisterFile()
            i2cSwitch.isChecked = app.sharedPrefs.getString("checkin_with_i2c_battery","false") == "true"
            setPermissionStatus(app)
        } else {
            record_group.visibility = View.INVISIBLE
            start_stop_group.visibility = View.INVISIBLE
            registerButton.visibility = View.VISIBLE
            registerInfo.visibility = View.INVISIBLE
            i2cSwitch.visibility = View.INVISIBLE
            permissionInfoLayout.visibility = View.INVISIBLE
        }
    }

    private fun setPermissionStatus(app: RfcxGuardian){
        if(app.isLocationEnabled){
            gpsStatusTextView.also {
                it.text = " on"
                it.setTextColor(resources.getColor(R.color.primary))
            }
        }else{
            gpsStatusTextView.also {
                it.text = " off"
                it.setTextColor(resources.getColor(R.color.grey_default))
            }
        }
    }

    private fun setVisibilityBeforeRegister() {
        record_group.visibility = View.INVISIBLE
        start_stop_group.visibility = View.INVISIBLE
        registerProgress.visibility = View.VISIBLE
    }

    private fun setVisibilityRegisterSuccess() {
        registerButton.visibility = View.INVISIBLE
        registerInfo.visibility = View.VISIBLE
        record_group.visibility = View.VISIBLE
        start_stop_group.visibility = View.VISIBLE
        registerProgress.visibility = View.INVISIBLE
    }

    private fun setVisibilityRegisterFailed() {
        registerButton.visibility = View.VISIBLE
        registerInfo.visibility = View.INVISIBLE
        record_group.visibility = View.INVISIBLE
        start_stop_group.visibility = View.INVISIBLE
        registerProgress.visibility = View.INVISIBLE
    }

    private fun getCheckinInformation(app: RfcxGuardian) {
        val checkInUtils = CheckInInformationUtils()
        getInfoThread = object : Thread() {
            override fun run() {
                try {
                    Log.d("getInfoThread", "Started")
                    while (!isInterrupted) {
                        runOnUiThread {
                            val latestRow = app.apiCheckInDb.dbSent.latestRow
                            if(latestRow[0] == null){
                                checkInText.text = checkInUtils.convertTimeStampToStringFormat(null)
                            }else{
                                val checkinTime = DateTimeUtils.getDateFromString(latestRow[0]).time
                                checkInText.text = checkInUtils.convertTimeStampToStringFormat(checkinTime)
                            }

                            if(latestRow[4] == null){
                                sizeText.text = checkInUtils.convertFileSizeToStringFormat(null)
                            }else{
                                val audioPath = latestRow[4]
                                sizeText.text = checkInUtils.convertFileSizeToStringFormat(audioPath)
                            }
                        }
                        sleep(5000)
                    }
                } catch (e: InterruptedException) {
                    Log.d("getInfoThread", "Interrupted")
                }
            }
        }
        getInfoThread?.start()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun isGuidExisted(): Boolean {
        val path = this.filesDir.toString()+"/txt/"
        val txtFile = File(path + "/guardian_guid.txt")
        return txtFile.exists()
    }

    private fun createRegisterFile(app: RfcxGuardian) {
        val path = this.filesDir.toString()+"/txt/"
        val file = File(path, "guardian_guid.txt")
        FileOutputStream(file).use {
            it.write(app.rfcxDeviceGuid.deviceGuid.toByteArray())
        }
    }

    private fun readRegisterFile(): String {
        val path = this.filesDir.toString()+"/txt/"
        val file = File(path, "guardian_guid.txt")
        return FileInputStream(file).bufferedReader().use { it.readText() }
    }

    override fun onPause() {
        super.onPause()
        getInfoThread?.interrupt()
    }

    companion object {
        private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, MainActivity::class.java)
        fun startActivity(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}
