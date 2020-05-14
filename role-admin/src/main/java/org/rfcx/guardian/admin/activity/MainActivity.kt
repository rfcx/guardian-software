package org.rfcx.guardian.admin.activity

import org.rfcx.guardian.admin.R

import org.rfcx.guardian.admin.RfcxGuardian
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.admin.sms.SmsUtils
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var monitorTimer: Timer
    private var isConnected: Boolean = false

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val app = application as RfcxGuardian
        when (item.itemId) {

            R.id.menu_sms ->  SmsUtils.testSmsQueue(app.rfcxPrefs.getPrefAsString("api_sms_address"), 75, 5, app.applicationContext)

            R.id.menu_screenshot -> app.rfcxServiceHandler.triggerService("ScreenShotCapture", true)

            R.id.menu_sntp -> app.rfcxServiceHandler.triggerService("SntpSyncJob", true)

            R.id.menu_logcat -> app.rfcxServiceHandler.triggerService("LogCatCapture", true)

            R.id.menu_reboot -> app.rfcxServiceHandler.triggerService("RebootTrigger", true)

        }
        return true
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)
        val app = application as RfcxGuardian
    }

    public override fun onResume() {
        super.onResume()
        val app = application as RfcxGuardian



        monitorToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val timerTask = object : TimerTask() {
                    override fun run() {
                        runOnUiThread {

                            i2cConnectStatusTextView.text = "disconnected"
                            systemResultTextView.text = "not found"
                            inputResultTextView.text = "not found"
                            batteryResultTextView.text = "not found"

                        }
                    }
                }
                monitorTimer = Timer()
                monitorTimer.schedule(timerTask, 0, 1000)
            } else {
                Log.d("toggle", "$isChecked")
                monitorTimer.cancel()
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        if (::monitorTimer.isInitialized) {
            monitorTimer.cancel()
        }
        (application as RfcxGuardian).appPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
