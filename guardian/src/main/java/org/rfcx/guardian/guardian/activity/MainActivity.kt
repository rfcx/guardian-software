package org.rfcx.guardian.guardian.activity

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.guardian.BuildConfig
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.register.ApiInterface
import org.rfcx.guardian.guardian.register.RegisterApi
import org.rfcx.guardian.guardian.entity.GuardianResponse
import org.rfcx.guardian.guardian.entity.RegisterRequest
import org.rfcx.guardian.guardian.manager.PreferenceManager
import org.rfcx.guardian.guardian.manager.getTokenID
import org.rfcx.guardian.guardian.manager.getUserNickname
import org.rfcx.guardian.guardian.manager.isLoginExpired
import org.rfcx.guardian.guardian.receiver.PhoneNumberRegisterDeliverReceiver
import org.rfcx.guardian.guardian.receiver.PhoneNumberRegisterSentReceiver
import org.rfcx.guardian.guardian.receiver.SmsDeliverListener
import org.rfcx.guardian.guardian.receiver.SmsSentListener
import org.rfcx.guardian.guardian.utils.CheckInInformationUtils
import org.rfcx.guardian.guardian.utils.PhoneNumberRegisterUtils
import org.rfcx.guardian.utility.datetime.DateTimeUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var getInfoThread: Thread? = null
    private lateinit var phoneNumberRegisterDeliverReceiver: PhoneNumberRegisterDeliverReceiver
    private lateinit var phoneNumberRegisterSentReceiver: PhoneNumberRegisterSentReceiver
    private lateinit var app: RfcxGuardian

    override fun onResume() {
        super.onResume()

        setVisibilityByPrefs(app)
        setUIByLoginState()
        setUIByGuidState(app)
        startServices()
        phoneRegisterSetup()
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
        initUI()

        app = application as RfcxGuardian

        Log.d("gps", app.isLocationEnabled.toString())
        startButton.setOnClickListener {
            if (!isGuidExisted()) {
                showToast("Please register this guardian first")
            } else if (!app.isLocationEnabled) {
                showToast("Please enable gps location")
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
                                    ApiInterface.create(baseContext)
                                        .isGuardianRegistered("Bearer ${getTokenID()}", guid)
                                        .enqueue(object :
                                            Callback<GuardianResponse> {
                                            override fun onFailure(
                                                call: Call<GuardianResponse>,
                                                t: Throwable
                                            ) {
                                                showToast("Try again later")
                                            }

                                            override fun onResponse(
                                                call: Call<GuardianResponse>,
                                                response: Response<GuardianResponse>
                                            ) {
                                                if (response.isSuccessful) {
                                                    createRegisterFile(app)
                                                    setUIByRecordingState(app)
                                                    setUIByGuidState(app)
                                                    setVisibilityRegisterSuccess()
                                                    deviceIdText.text = readRegisterFile()
                                                    app.startAllServices()
                                                    setUIFromBtnClicked("start")
                                                    getCheckinInformation(app)
                                                } else {
                                                    showToast("Try again later")
                                                }
                                            }
                                        })
                                }

                                override fun onFailed(t: Throwable?, message: String?) {
                                    setVisibilityRegisterFailed()
                                    showToast(message ?: "register failed")
                                    Log.d("register_failed", t.toString())
                                }

                            })
                    } else {
                        registerButton.visibility = View.INVISIBLE
                        registerInfo.visibility = View.VISIBLE
                    }
                }
            } else {
                showToast("There is not internet connection. Please turn it on.")
            }
        }

        phoneNumRegisterButton.setOnClickListener {
            PhoneNumberRegisterUtils.sendSMS(this)
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

    }

    private fun initUI() {
        toolBarInit()
        setAppVersion()
    }

    private fun toolBarInit() {
        val toolbar = supportActionBar
        toolbar?.title = "Guardian"
    }

    private fun setAppVersion() {
        val versionName = BuildConfig.VERSION_NAME
        appVersionText.text = "version: $versionName"
    }

    private fun phoneRegisterSetup() {
        phoneNumberRegisterDeliverReceiver =
            PhoneNumberRegisterDeliverReceiver(object : SmsDeliverListener {
                override fun onDelivered(isSuccess: Boolean) {
                    if (isSuccess) {
                        showPhoneRegisterStatus()
                        PreferenceManager.getInstance(applicationContext)
                            .putBoolean("PHONE_REGISTER", true)
                        showToast("registered success")
                    } else {
                        showToast("please try again")
                    }
                }
            })

        phoneNumberRegisterSentReceiver = PhoneNumberRegisterSentReceiver(object : SmsSentListener {
            override fun onSent(message: String) {
                showToast(message)
            }
        })

        if (PreferenceManager.getInstance(applicationContext).getBoolean("PHONE_REGISTER", false)) {
            showPhoneRegisterStatus()
        }
        registerReceiver(phoneNumberRegisterDeliverReceiver, IntentFilter("SMS_DELIVERED"))
        registerReceiver(phoneNumberRegisterSentReceiver, IntentFilter("SMS_SENT"))
    }

    private fun showPhoneRegisterStatus() {
        phoneNumRegisterButton.visibility = View.GONE
        phoneNumRegisterStatusTextView.visibility = View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    private fun startServices() {
        Handler().postDelayed({
            app.startAllServices()
            setUIByRecordingState(app)
            setBtnEnableByRecordingState(app)
            if (app.recordingState) {
                getCheckinInformation(app)
            }
        }, 1000)
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
            recordStatusText.text = "recording"
            recordStatusText.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            startButton.isEnabled = true
            stopButton.isEnabled = false
            recordStatusText.text = "not recording"
            recordStatusText.setTextColor(ContextCompat.getColor(this, R.color.grey_default))
        }
    }

    private fun setVisibilityByPrefs(app: RfcxGuardian) {
        if (app.rfcxPrefs.getPrefAsString("show_ui") == "false") {
            rootView.visibility = View.INVISIBLE
        } else {
            rootView.visibility = View.VISIBLE
        }
    }

    private fun setUIByRecordingState(app: RfcxGuardian) {
        if (isGuidExisted()) {
            Log.d("Guid", "existed")
            deviceIdText.text = readRegisterFile()
            if (app.recordingState) {
                recordStatusText.text = "recording"
                recordStatusText.setTextColor(ContextCompat.getColor(this, R.color.primary))
            } else {
                recordStatusText.text = "not recording"
                recordStatusText.setTextColor(ContextCompat.getColor(this, R.color.grey_default))
            }
        } else {
            Log.d("Guid", "not existed")
        }
    }

    private fun setUIByLoginState() {
        if (this.isLoginExpired()) {
            loginButton.visibility = View.VISIBLE
            loginInfo.visibility = View.INVISIBLE
            start_stop_button.visibility = View.INVISIBLE
            registerButton.isEnabled = false
            registerButton.alpha = 0.5f
        } else {
            loginButton.visibility = View.INVISIBLE
            loginInfo.visibility = View.VISIBLE
            start_stop_button.visibility = View.VISIBLE
            registerButton.isEnabled = true
            registerButton.alpha = 1.0f
            userName.text = this.getUserNickname()
        }
    }

    private fun setUIByGuidState(app: RfcxGuardian) {
        if (isGuidExisted()) {
            start_stop_group.visibility = View.VISIBLE
            registerButton.visibility = View.INVISIBLE
            start_stop_button.visibility = View.VISIBLE
            registerInfo.visibility = View.VISIBLE
            switchView.visibility = View.VISIBLE
            permissionInfoLayout.visibility = View.VISIBLE
            deviceIdText.text = readRegisterFile()
            setPermissionStatus(app)
            appVersionText.visibility = View.VISIBLE
        } else {
            start_stop_group.visibility = View.INVISIBLE
            registerButton.visibility = View.VISIBLE
            start_stop_button.visibility = View.INVISIBLE
            registerInfo.visibility = View.INVISIBLE
            switchView.visibility = View.INVISIBLE
            permissionInfoLayout.visibility = View.INVISIBLE
            appVersionText.visibility = View.INVISIBLE
        }
    }

    private fun setPermissionStatus(app: RfcxGuardian) {
        if (app.isLocationEnabled) {
            gpsStatusTextView.also {
                it.text = " on"
                it.setTextColor(resources.getColor(R.color.primary))
            }
        } else {
            gpsStatusTextView.also {
                it.text = " off"
                it.setTextColor(resources.getColor(R.color.grey_default))
            }
        }
    }

    private fun setVisibilityBeforeRegister() {
        start_stop_group.visibility = View.INVISIBLE
        registerProgress.visibility = View.VISIBLE
    }

    private fun setVisibilityRegisterSuccess() {
        registerButton.visibility = View.INVISIBLE
        registerInfo.visibility = View.VISIBLE
        start_stop_button.visibility = View.VISIBLE
        start_stop_group.visibility = View.VISIBLE
        registerProgress.visibility = View.INVISIBLE
    }

    private fun setVisibilityRegisterFailed() {
        registerButton.visibility = View.VISIBLE
        registerInfo.visibility = View.INVISIBLE
        start_stop_button.visibility = View.INVISIBLE
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
                            if (latestRow[0] == null) {
                                checkInText.text = checkInUtils.convertTimeStampToStringFormat(null)
                            } else {
                                val checkinTime = DateTimeUtils.getDateFromString(latestRow[0]).time
                                checkInText.text =
                                    checkInUtils.convertTimeStampToStringFormat(checkinTime)
                            }

                            if (latestRow[4] == null) {
                                sizeText.text = checkInUtils.convertFileSizeToStringFormat(null)
                            } else {
                                val audioPath = latestRow[4]
                                sizeText.text =
                                    checkInUtils.convertFileSizeToStringFormat(audioPath)
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
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun isGuidExisted(): Boolean {
        val path = this.filesDir.toString() + "/txt/"
        val txtFile = File(path + "/guardian_guid.txt")
        return txtFile.exists()
    }

    private fun createRegisterFile(app: RfcxGuardian) {
        val path = this.filesDir.toString() + "/txt/"
        val file = File(path, "guardian_guid.txt")
        FileOutputStream(file).use {
            it.write(app.rfcxDeviceGuid.deviceGuid.toByteArray())
        }
    }

    private fun readRegisterFile(): String {
        val path = this.filesDir.toString() + "/txt/"
        val file = File(path, "guardian_guid.txt")
        return FileInputStream(file).bufferedReader().use { it.readText() }
    }

    override fun onPause() {
        super.onPause()
        getInfoThread?.interrupt()
        unregisterReceiver(phoneNumberRegisterDeliverReceiver)
        unregisterReceiver(phoneNumberRegisterSentReceiver)
    }

    companion object {
        private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, MainActivity::class.java)
        fun startActivity(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}
