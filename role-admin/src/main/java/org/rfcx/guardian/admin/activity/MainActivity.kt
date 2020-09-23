package org.rfcx.guardian.admin.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_home.*
import org.rfcx.guardian.admin.R
import org.rfcx.guardian.admin.RfcxGuardian
import org.rfcx.guardian.admin.sms.SmsUtils

class MainActivity : Activity(), TextWatcher {

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

        smsMsgEditText.addTextChangedListener(this)
        smsNumberEditText.addTextChangedListener(this)

        smsNumberEditText.setText("${app.rfcxPrefs.getPrefAsString("api_sms_address")}")

        sendMsgButton.setOnClickListener {
            //message for sending
            val msg = smsMsgEditText.text.toString()
            val number = smsNumberEditText.text.toString()
            SmsUtils.addImmediateSmsToQueue(number, msg, app.applicationContext)
            smsMsgEditText.text = null
            smsNumberEditText.text = null
        }

        sntpSyncButton.setOnClickListener {
            app.rfcxServiceHandler.triggerService("SntpSyncJob", true)
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

        if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
            // App is not default.
            // Show the "not currently set as the default SMS app" interface
            notDefaultAppText.visibility = View.VISIBLE

            // Set up a button that allows the user to change the default SMS app
            changeDefaultAppButton.setOnClickListener {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(
                    Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                    packageName
                )
                startActivity(intent)
            }
        } else {
            // App is the default.
            // Hide the "not currently set as the default SMS app" interface
            changeDefaultAppButton.visibility = View.GONE
            notDefaultAppText.visibility = View.GONE

            smsMsgEditText.visibility = View.VISIBLE
            smsNumberEditText.visibility = View.VISIBLE
            sendMsgButton.visibility = View.VISIBLE

            sntpSyncButton.visibility = View.VISIBLE
            screenshotButton.visibility = View.VISIBLE
            rebootButton.visibility = View.VISIBLE
        }
    }

    public override fun onPause() {
        super.onPause()
        (application as RfcxGuardian).appPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun afterTextChanged(p0: Editable?) {
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        sendMsgButton.isEnabled = !p0.isNullOrBlank()
    }

}
