package org.rfcx.guardian.admin.sms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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

					JSONObject smsObj = smsJsonArray.getJSONObject(i);
					String msgId = DeviceSmsUtils.generateMessageId();
					app.smsMessageDb.dbSmsReceived.insert(smsObj.getString("received_at"), smsObj.getString("address"), smsObj.getString("body"), msgId);
					Log.w(logTag, "SMS Received (ID "+msgId+"): From "+smsObj.getString("address")+" at "+smsObj.getString("received_at")+": \""+smsObj.getString("body")+"\"");

				} catch (JSONException e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}
    }
    
}