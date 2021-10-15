package org.rfcx.guardian.admin.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.admin.R
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.device.android.capture.ScreenShotCaptureService
import org.rfcx.guardian.admin.device.android.control.ClockSyncJobService
import org.rfcx.guardian.admin.device.android.control.RebootTriggerService
import org.rfcx.guardian.admin.comms.sms.ComposeSmsActivity

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
            app.rfcxSvc.triggerService(ClockSyncJobService.SERVICE_NAME, true)
        }

        screenshotButton.setOnClickListener {
            app.rfcxSvc.triggerService(ScreenShotCaptureService.SERVICE_NAME, true)
        }

        rebootButton.setOnClickListener {
            app.rfcxSvc.triggerService(RebootTriggerService.SERVICE_NAME, true)
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
