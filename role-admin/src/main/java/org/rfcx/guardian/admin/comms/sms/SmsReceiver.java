package org.rfcx.guardian.admin.comms.sms;

import org.json.JSONArray;
import org.json.JSONException;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;

public class SmsReceiver extends BroadcastReceiver {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SmsReceiver");
	
    @Override
    public void onReceive(Context context, Intent intent) {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

		if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {

			JSONArray smsJsonArray = DeviceSmsUtils.processIncomingSmsMessageAsJson(intent);
			for (int i = 0; i < smsJsonArray.length(); i++) {
				try {

					SmsUtils.processIncomingSms(smsJsonArray.getJSONObject(i), context);

				} catch (JSONException e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}
    }
    
}
