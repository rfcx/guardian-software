package org.rfcx.guardian.admin.receiver;

import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.admin.RfcxGuardian;

public class SmsReceiver extends BroadcastReceiver {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, SmsReceiver.class);
	
    @Override
    public void onReceive(Context context, Intent intent) {

    		if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
    			
    			String smsJsonString = DeviceSmsUtils.processIncomingSmsMessageAsJson(intent);
    			if (smsJsonString != null) {
    				Log.i(logTag, "SMS Received: "+smsJsonString);
    			}
        }
    }
    
}
