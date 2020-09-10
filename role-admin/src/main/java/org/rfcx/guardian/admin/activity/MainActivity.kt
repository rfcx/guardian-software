package org.rfcx.guardian.admin.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.admin.R
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.sms.ComposeSmsActivity

class MainActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val app = application as RfcxGuardian

        smsActivityButton.setOnClickListener {
            startActivity(Intent(this, ComposeSmsActivity::class.java))
        }

        screenshotButton.setOnClickListener {
            app.rfcxServiceHandler.triggerService("ScreenShotCapture", true)
        }

        sntpButton.setOnClickListener {
            app.rfcxServiceHandler.triggerService("SntpSyncJob", true)
        }

        logcatButton.setOnClickListener {
            app.rfcxServiceHandler.triggerService("LogCatCapture", true)
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
