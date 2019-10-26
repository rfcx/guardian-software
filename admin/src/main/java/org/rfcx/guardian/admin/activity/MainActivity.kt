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
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_home.*

class MainActivity : AppCompatActivity() {


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

            R.id.menu_i2c_view -> {
                val result = app.sentinelPowerUtils.saveSentinelPowerValuesToDatabaseWithResult(
                    app.applicationContext,
                    true
                )
                if(result.isNotEmpty()){
                    val listOfValue = result.joinToString("\n")
                    i2cResultTextView.text = listOfValue
                }else{
                    i2cResultTextView.text = "i2c value not found"
                }
            }
            R.id.menu_logcat -> app.rfcxServiceHandler.triggerService("LogCatCapture", true)
        }
        return true
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
        (application as RfcxGuardian).appPause()
    }

}
