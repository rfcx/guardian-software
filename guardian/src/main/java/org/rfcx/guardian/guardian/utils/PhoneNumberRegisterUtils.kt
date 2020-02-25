package org.rfcx.guardian.guardian.utils

import android.telephony.SmsManager
import android.content.Intent
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import java.lang.Exception


object PhoneNumberRegisterUtils {

    fun sendSMS(context: Context) {
        try {
            val phoneNumber = "" // left for phone number
            val message = "sms sending test"
            val smsManager = SmsManager.getDefault()
            val deliveredIntent = PendingIntent.getBroadcast(context, 0, Intent("SMS_DELIVERED"), 0)
            val sentIntent = PendingIntent.getBroadcast(context, 0, Intent("SMS_SENT"), 0)
            smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, deliveredIntent)
        } catch (e: Exception){
            Log.d("PhoneNumberRegisterUtil", e.toString())
        }
    }
}
