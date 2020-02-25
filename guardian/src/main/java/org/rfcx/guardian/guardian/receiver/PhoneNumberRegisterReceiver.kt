package org.rfcx.guardian.guardian.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager

interface SmsDeliverListener{
    fun onDelivered(isSuccess: Boolean)
}

interface SmsSentListener{
    fun onSent(message: String)
}

class PhoneNumberRegisterDeliverReceiver(private val callback: SmsDeliverListener) : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        var isSuccess = false
        when(resultCode){
            Activity.RESULT_OK -> isSuccess = true
        }
        callback.onDelivered(isSuccess)
    }
}

class PhoneNumberRegisterSentReceiver(private val callback: SmsSentListener) : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        var message = ""
        when(resultCode){
            Activity.RESULT_OK -> message = "registration sent"
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> message = "generic failure"
            SmsManager.RESULT_ERROR_NO_SERVICE -> message = "no service"
            SmsManager.RESULT_ERROR_NULL_PDU -> message = "no pdu"
            SmsManager.RESULT_ERROR_RADIO_OFF -> message = "radio off"
        }
        callback.onSent(message)
    }
}
