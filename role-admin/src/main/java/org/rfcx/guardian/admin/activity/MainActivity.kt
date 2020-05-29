package org.rfcx.guardian.admin.activity

import android.content.Intent
import org.rfcx.guardian.admin.R

import org.rfcx.guardian.admin.RfcxGuardian
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.admin.sms.ComposeSmsActivity
import org.rfcx.guardian.admin.sms.SmsUtils
import java.util.*

class MainActivity : AppCompatActivity() {

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

            R.id.menu_sms_activity -> startActivity(Intent(this, ComposeSmsActivity::class.java))

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
        (application as RfcxGuardian).appResume()
    }

    public override fun onPause() {
        super.onPause()
        (application as RfcxGuardian).appPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
