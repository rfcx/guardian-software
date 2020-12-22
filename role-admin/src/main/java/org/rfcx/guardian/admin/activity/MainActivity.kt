package org.rfcx.guardian.admin.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.admin.R
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.sms.ComposeSmsActivity

class MainActivity : Activity() {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
        }
        return true
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val app = application as RfcxGuardian

        openSmsActivityButton.setOnClickListener {
            startActivity(Intent(this, ComposeSmsActivity::class.java))
        }

        clockSyncButton.setOnClickListener {
            app.rfcxServiceHandler.triggerService("ClockSyncJob", true)
        }

        screenshotButton.setOnClickListener {
            app.rfcxServiceHandler.triggerService("ScreenShotCapture", true)
        }

        rebootButton.setOnClickListener {
            app.rfcxServiceHandler.triggerService("RebootTrigger", true)
        }

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
