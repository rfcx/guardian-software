package org.rfcx.guardian.guardian.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import org.rfcx.guardian.guardian.utils.CheckInInformationUtils
import org.rfcx.guardian.guardian.utils.GuardianUtils
import org.rfcx.guardian.utility.rfcx.RfcxLog


class MainActivity : AppCompatActivity(), RegisterCallback, GuardianCheckCallback {
    private var getInfoThread: Thread? = null
    private lateinit var app: RfcxGuardian

    override fun onResume() {
        super.onResume()

        setConfiguration()
        setVisibilityByPrefs()
        setUIByLoginState()
        setUIByGuidState()
        startServices()
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
            if (!GuardianUtils.isNetworkAvailable(this)) {
                showToast("There is not internet connection. Please turn it on.")
                return@setOnClickListener
            }
            app.initializeRoleServices()
            setUIFromBtnClicked("start")
            getCheckinInformation()
        }

        stopButton.setOnClickListener {
            app.rfcxServiceHandler.stopAllServices()
            app.rfcxServiceHandler.setAbsoluteRunState("OnLaunchServiceSequence", false)
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
            val guid = app.rfcxGuardianIdentity.guid
            val token = app.rfcxGuardianIdentity.authToken

            RegisterApi.registerGuardian(applicationContext, RegisterRequest(guid, token), this)
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

    private fun setConfiguration() {
        val sampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate")
        rgSampleRate.check(
            when (sampleRate) {
                8000 -> R.id.rb8Hz
                12000 -> R.id.rb12Hz
                24000 -> R.id.rb24Hz
                else -> R.id.rb48Hz
            }
        )
        rgSampleRate.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb8Hz -> app.setSharedPref("audio_sample_rate", "8000")
                R.id.rb12Hz -> app.setSharedPref("audio_sample_rate", "12000")
                R.id.rb24Hz -> app.setSharedPref("audio_sample_rate", "24000")
                R.id.rb48Hz -> app.setSharedPref("audio_sample_rate", "48000")
            }
        }

        val fileFormat = app.rfcxPrefs.getPrefAsString("audio_encode_codec")
        rgFileFormat.check(
            when (fileFormat) {
                "opus" -> R.id.rbOpus
                else -> R.id.rbFlac
            }
        )
        rgFileFormat.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbOpus -> app.setSharedPref("audio_encode_codec", "opus")
                R.id.rbFlac -> app.setSharedPref("audio_encode_codec", "flac")
            }
        }

        val duration = app.rfcxPrefs.getPrefAsString("audio_cycle_duration")
        durationEditText.setText(duration)
        durationEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                app.setSharedPref("audio_cycle_duration", s.toString())
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    private fun startServices() {
        Handler().postDelayed({
            app.initializeRoleServices()
            setUIByRecordingState()
            setBtnEnableByRecordingState()
            if (app.rfcxServiceHandler.isRunning("AudioCapture")) {
                getCheckinInformation()
            }
        }, 1000)
    }

    private fun setBtnEnableByRecordingState() {
        when (app.rfcxServiceHandler.isRunning("AudioCapture")) {
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
            setConfigurationByRecordingState(true)
        } else {
            startButton.isEnabled = true
            stopButton.isEnabled = false
            recordStatusText.text = "not recording"
            recordStatusText.setTextColor(ContextCompat.getColor(this, R.color.grey_default))
            setConfigurationByRecordingState(false)
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
        if (GuardianUtils.isGuardianRegistered(this)) {
            deviceIdText.text = GuardianUtils.readGuardianGuid(this)
            if (app.rfcxServiceHandler.isRunning("AudioCapture")) {
                recordStatusText.text = "recording"
                recordStatusText.setTextColor(ContextCompat.getColor(this, R.color.primary))
                setConfigurationByRecordingState(true)
            } else {
                recordStatusText.text = "not recording"
                recordStatusText.setTextColor(ContextCompat.getColor(this, R.color.grey_default))
                setConfigurationByRecordingState(false)
            }
        }
    }

    private fun setConfigurationByRecordingState(state: Boolean) {
        if (state) {
            configurationLayout.alpha = 0.5f
            setRadioGroupState(false, rgSampleRate)
            setRadioGroupState(false, rgFileFormat)
            durationEditText.isEnabled = false
        } else {
            configurationLayout.alpha = 1.0f
            setRadioGroupState(true, rgSampleRate)
            setRadioGroupState(true, rgFileFormat)
            durationEditText.isEnabled = true
        }
    }

    private fun setRadioGroupState(state: Boolean, radioGroup: RadioGroup) {
        for (i in 0 until radioGroup.childCount) {
            radioGroup.getChildAt(i).isEnabled = state
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
        if (GuardianUtils.isGuardianRegistered(this)) {
            unregisteredView.visibility = View.INVISIBLE
            registeredView.visibility = View.VISIBLE
            registerProgress.visibility = View.INVISIBLE
            registerInfo.visibility = View.VISIBLE
            deviceIdText.text = GuardianUtils.readGuardianGuid(this)
        } else {
            unregisteredView.visibility = View.VISIBLE
            registeredView.visibility = View.INVISIBLE
            registerInfo.visibility = View.INVISIBLE
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

    private fun setVisibilityRegisterFail() {
        registerButton.visibility = View.VISIBLE
        registerProgress.visibility = View.INVISIBLE
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

                            val recordedList = app.diagnosticDb.dbRecordedDiagnostic.latestRow
                            val syncedList = app.diagnosticDb.dbSyncedDiagnostic.latestRow
                            recordTimeText.text =
                                app.diagnosticUtils.secondToTime(recordedList[2].toInt())
                            fileRecordedSyncedText.text = "${syncedList[1]} / ${recordedList[1]}"

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
        GuardianCheckApi.exists(applicationContext, app.rfcxGuardianIdentity.guid, this)
    }

    override fun onRegisterFailed(t: Throwable?, message: String?) {
        setVisibilityRegisterFail()
        showToast(message ?: "register failed")
    }

    override fun onGuardianCheckSuccess() {
        setVisibilityRegisterSuccess()
        GuardianUtils.createRegisterFile(baseContext)
        app.initializeRoleServices()
        setUIByRecordingState()
        setUIByGuidState()
        setUIFromBtnClicked("start")
        getCheckinInformation()
        deviceIdText.text = GuardianUtils.readGuardianGuid(baseContext)
    }

    override fun onGuardianCheckFailed(t: Throwable?, message: String?) {
        setVisibilityRegisterFail()
        showToast(message ?: "Try again later")
    }

    override fun onPause() {
        super.onPause()
        if (getInfoThread != null) {
            getInfoThread?.interrupt()
        }
    }

    companion object {
        private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, MainActivity::class.java)
    }
}
