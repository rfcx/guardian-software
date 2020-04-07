package org.rfcx.guardian.guardian.activity

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.api.GuardianCheckApi
import org.rfcx.guardian.guardian.api.GuardianCheckCallback
import org.rfcx.guardian.guardian.api.RegisterApi
import org.rfcx.guardian.guardian.api.RegisterCallback
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
import org.rfcx.guardian.guardian.utils.GuardianUtils
import org.rfcx.guardian.guardian.utils.PhoneNumberRegisterUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog

class MainActivity : AppCompatActivity(), RegisterCallback, GuardianCheckCallback {
    private var getInfoThread: Thread? = null
    private lateinit var phoneNumberRegisterDeliverReceiver: PhoneNumberRegisterDeliverReceiver
    private lateinit var phoneNumberRegisterSentReceiver: PhoneNumberRegisterSentReceiver
    private lateinit var app: RfcxGuardian

    override fun onResume() {
        super.onResume()

        setVisibilityByPrefs()
        setUIByLoginState()
        setUIByGuidState()
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
        app = application as RfcxGuardian

        setSupportActionBar(toolbar)
        initUI()

        startButton.setOnClickListener {
            if (!app.isLocationEnabled) {
                showToast("Please enable gps location")
            } else {
                app.startAllServices()
                setUIFromBtnClicked("start")
                getCheckinInformation()
            }
        }

        stopButton.setOnClickListener {
            app.rfcxServiceHandler.stopAllServices()
            getInfoThread?.interrupt()
            setUIFromBtnClicked("stop")
        }

        registerButton.setOnClickListener {
            if (!GuardianUtils.isNetworkAvailable(this)) {
                showToast("There is not internet connection. Please turn it on.")
                return@setOnClickListener
            }

            if (this.getTokenID() == null) {
                showToast("Please login before register guardian.")
                return@setOnClickListener
            }

            setVisibilityBeforeRegister()
            val guid = app.rfcxDeviceGuid.deviceGuid
            val token = app.rfcxDeviceGuid.deviceToken

            RegisterApi.registerGuardian(applicationContext, RegisterRequest(guid, token), this)
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
        appVersionText.text = "version: ${app.version}"
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
            setUIByRecordingState()
            setBtnEnableByRecordingState()
            if (app.recordingState) {
                getCheckinInformation()
            }
        }, 1000)
    }

    private fun setBtnEnableByRecordingState() {
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

    private fun setVisibilityByPrefs() {
        if (app.rfcxPrefs.getPrefAsString("show_ui") == "true") {
            rootView.visibility = View.VISIBLE
        } else {
            rootView.visibility = View.INVISIBLE
        }
    }

    private fun setUIByRecordingState() {
        if (GuardianUtils.isGuidExisted(this)) {
            deviceIdText.text = GuardianUtils.readRegisterFile(this)
            if (app.recordingState) {
                recordStatusText.text = "recording"
                recordStatusText.setTextColor(ContextCompat.getColor(this, R.color.primary))
            } else {
                recordStatusText.text = "not recording"
                recordStatusText.setTextColor(ContextCompat.getColor(this, R.color.grey_default))
            }
        }
    }

    private fun setUIByLoginState() {
        if (this.isLoginExpired()) {
            loginInfo.visibility = View.INVISIBLE
            loginButton.visibility = View.VISIBLE
        } else {
            loginInfo.visibility = View.VISIBLE
            loginButton.visibility = View.INVISIBLE
            userName.text = this.getUserNickname()
        }
    }

    private fun setUIByGuidState() {
        if (GuardianUtils.isGuidExisted(this)) {
            unregisteredView.visibility = View.INVISIBLE
            registeredView.visibility = View.VISIBLE
            registerProgress.visibility = View.INVISIBLE
            deviceIdText.text = GuardianUtils.readRegisterFile(this)
            setPermissionStatus()
        } else {
            unregisteredView.visibility = View.VISIBLE
            registeredView.visibility = View.INVISIBLE
        }
    }

    private fun setPermissionStatus() {
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
        registerButton.visibility = View.INVISIBLE
        registerProgress.visibility = View.VISIBLE
    }

    private fun setVisibilityRegisterSuccess() {
        registeredView.visibility = View.VISIBLE
        unregisteredView.visibility = View.INVISIBLE
    }

    private fun getCheckinInformation() {
        val checkInUtils = CheckInInformationUtils()
        getInfoThread = object : Thread() {
            override fun run() {
                try {
                    while (!isInterrupted) {
                        runOnUiThread {
                            val latestRow = app.apiCheckInDb.dbSent.latestRow
                            checkInText.text = checkInUtils.getCheckinTime(latestRow[0])
                            sizeText.text = checkInUtils.getFileSize(latestRow[4])
                        }
                        sleep(5000)
                    }
                } catch (e: InterruptedException) {
                    return
                }
            }
        }
        getInfoThread?.start()
    }

    override fun onRegisterSuccess() {
        GuardianCheckApi.exists(applicationContext, app.rfcxDeviceGuid.deviceGuid, this)
    }

    override fun onRegisterFailed(t: Throwable?, message: String?) {
        showToast(message ?: "register failed")
    }

    override fun onGuardianCheckSuccess() {
        setVisibilityRegisterSuccess()
        GuardianUtils.createRegisterFile(baseContext)
        app.startAllServices()
        setUIByRecordingState()
        setUIByGuidState()
        setUIFromBtnClicked("start")
        getCheckinInformation()
        deviceIdText.text = GuardianUtils.readRegisterFile(baseContext)
    }

    override fun onGuardianCheckFailed(t: Throwable?, message: String?) {
        showToast(message ?: "Try again later")
    }

    override fun onPause() {
        super.onPause()
        if (getInfoThread != null) {
            getInfoThread?.interrupt()
        }
        unregisterReceiver(phoneNumberRegisterDeliverReceiver)
        unregisterReceiver(phoneNumberRegisterSentReceiver)
    }

    companion object {
        private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, MainActivity::class.java)
    }
}
