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

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, SmsReceiver.class);
	
    @Override
    public void onReceive(Context context, Intent intent) {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

		Log.w(logTag, "Broadcast: "+intent.getAction());

		if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {

			JSONArray smsJsonArray = DeviceSmsUtils.processIncomingSmsMessageAsJson(intent);
			for (int i = 0; i < smsJsonArray.length(); i++) {
				try {

					JSONObject smsObj = smsJsonArray.getJSONObject(i);
					int msgId = (int) (Math.random() * 10000 + 1);
					app.deviceSmsMessageDb.dbSmsReceived.insert(smsObj.getString("received_at"), smsObj.getString("address"), smsObj.getString("body"), ""+msgId);
					Log.w(logTag, "SMS Received (ID "+msgId+"): From "+smsObj.getString("address")+" at "+smsObj.getString("received_at")+": \""+smsObj.getString("body")+"\"");

				//	DeviceSmsUtils.sendSmsMessage("+14153359205", "Hello! This is a test: "+System.currentTimeMillis());

				} catch (JSONException e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}/* else if (intent.getAction().equals("android.provider.Telephony.SMS_DELIVER")) {

			JSONArray smsJsonArray = DeviceSmsUtils.processIncomingSmsMessageAsJson(intent);
			for (int i = 0; i < smsJsonArray.length(); i++) {
				try {

					JSONObject smsObj = smsJsonArray.getJSONObject(i);

					int msgId = (int) (Math.random() * 1000 + 1);
					//

					Log.w(logTag, "SMS Delivered (ID "+msgId+"): From "+smsObj.getString("address")+" at "+smsObj.getString("received_at")+": \""+smsObj.getString("body")+"\"");

	//				app.deviceSmsMessageDb.dbSmsSent.insert(smsObj.getString("received_at"), smsObj.getString("address"), smsObj.getString("body"), smsObj.getString("received_at"));


//					Log.w(logTag, "SMS Received: From "+smsObj.getString("address")+" at "+smsObj.getString("received_at")+": \""+smsObj.getString("body")+"\"");

		//			DeviceSmsUtils.sendSmsMessage("+14153359205", "Hello! This is a test: "+System.currentTimeMillis());

				} catch (JSONException e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}*/
    }
    
}
