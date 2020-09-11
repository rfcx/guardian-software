package org.rfcx.guardian.admin.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import org.rfcx.guardian.admin.R
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.sms.ComposeSmsActivity

class MainActivity : Activity() {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val app = application as RfcxGuardian
        when (item.itemId) {
            R.id.menu_sms_activity -> startActivity(Intent(this, ComposeSmsActivity::class.java))
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
