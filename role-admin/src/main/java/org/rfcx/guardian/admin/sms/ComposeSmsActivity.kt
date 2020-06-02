package org.rfcx.guardian.admin.sms

import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_compose_sms.*
import org.rfcx.guardian.admin.R
import org.rfcx.guardian.admin.RfcxGuardian

class ComposeSmsActivity : AppCompatActivity(), TextWatcher {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_sms)

        val app = application as RfcxGuardian

        smsMsgEditText.addTextChangedListener(this)
        smsNumberEditText.addTextChangedListener(this)

        smsNumberEditText.setText("${app.rfcxPrefs.getPrefAsString("api_sms_address")}")

        sendMsgButton.setOnClickListener {
            //message for sending
            val msg = smsMsgEditText.text.toString()
            val number = smsNumberEditText.text.toString()
            SmsUtils.processSendingSms(
                number,
                msg,
                75,
                1,
                app.applicationContext
            )
            smsMsgEditText.text = null
            smsNumberEditText.text = null
        }
    }

    override fun onResume() {
        super.onResume()

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
        }
    }

    override fun afterTextChanged(p0: Editable?) {
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        sendMsgButton.isEnabled = !p0.isNullOrBlank()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
