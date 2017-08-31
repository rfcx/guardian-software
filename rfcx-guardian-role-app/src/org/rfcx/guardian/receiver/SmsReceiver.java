package org.rfcx.guardian.receiver;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {

	private static final String logTag = "RfcxGuardian-"+SmsReceiver.class.getSimpleName();
	
    @Override
    public void onReceive(Context context, Intent intent) {

    	if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
    		
    		Bundle bundle = intent.getExtras();
            if (bundle != null) {
            	
            	try {
            		Object[] pdus = (Object[]) bundle.get("pdus");
                    for (Object pdu : pdus) {
                    	SmsMessage msg = SmsMessage.createFromPdu( (byte[]) pdu );
                    	// do something...
                        Log.i(logTag, "SMS received from '"+msg.getOriginatingAddress()+"': "+msg.getMessageBody());
                    }
                } catch (Exception e) {
                	RfcxLog.logExc(logTag, e);
                }
            }
        }
    }
    
}
