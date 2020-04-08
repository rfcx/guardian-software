package org.rfcx.guardian.admin.activity

import android.content.Intent
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Button
import org.rfcx.guardian.admin.R

import org.rfcx.guardian.admin.RfcxGuardian
import android.app.Activity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.Menu
import android.Manifest
import android.view.MenuItem
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.admin.device.android.system.DeviceUtils
import org.rfcx.guardian.utility.device.control.DeviceBluetooth
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

            R.id.menu_reboot -> app.rfcxServiceHandler.triggerService("RebootTrigger", true)

            R.id.menu_relaunch -> app.rfcxServiceHandler.triggerIntentServiceImmediately("ForceRoleRelaunch")

            R.id.menu_screenshot -> app.rfcxServiceHandler.triggerService("ScreenShotCapture", true)

            R.id.menu_sntp -> app.rfcxServiceHandler.triggerService("DateTimeSntpSyncJob", true)

            R.id.menu_logcat -> app.rfcxServiceHandler.triggerService("LogCatCapture", true)

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
                            isConnected = app.sentinelPowerUtils.confirmConnection()
                            if (isConnected) {
                                i2cConnectStatusTextView.text = "connected"
                                val result = app.sentinelPowerUtils.latestSentinelValues
                                if (result != null) {
                                    systemResultTextView.text = result.system.toString()
                                    inputResultTextView.text = result.input.toString()
                                    batteryResultTextView.text = result.battery.toString()
                                }
                            } else {
                                i2cConnectStatusTextView.text = "disconnected"
                                systemResultTextView.text = "not found"
                                inputResultTextView.text = "not found"
                                batteryResultTextView.text = "not found"
                            }
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
