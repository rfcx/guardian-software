package org.rfcx.guardian.admin.sms;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SmsUtils {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SmsUtils");


	public static boolean testSmsQueue(String address, long delayInterval, int smsCount, Context context) {
		return sendSms(address, "", delayInterval, smsCount, context);
	}

	//with user's defined message
	public static boolean processSendingSms(String address, String message, long delayInterval, int smsCount, Context context) {
		return sendSms(address, message, delayInterval, smsCount, context);
	}

	private static boolean sendSms(String address, String message, long delayInterval, int smsCount, Context context) {
		long thisTime = System.currentTimeMillis();

		for (int i = 1; i <= smsCount; i++) {
			addScheduledSmsToQueue(thisTime, address, i+") SMS Test Message: " + ((!message.isEmpty()) ? message: DateTimeUtils.getDateTime(thisTime)), context);
			thisTime = thisTime + (delayInterval*1000);
		}

		return true;
	}

	// Incoming Message Tools

	public static void processIncomingSms(JSONObject smsObj, Context context) throws JSONException {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		String apiSmsAddress = app.rfcxPrefs.getPrefAsString("api_sms_address");

		if (!smsObj.getString("address").equalsIgnoreCase(apiSmsAddress)) {
			// In this case, since message did not arrive from the specified API SMS address, we just save the message.
			String msgId = DeviceSmsUtils.generateMessageId();
			app.smsMessageDb.dbSmsReceived.insert(smsObj.getString("received_at"), smsObj.getString("address"), smsObj.getString("body"), msgId);
			Log.w(logTag, "SMS Received (ID " + msgId + "): From " + smsObj.getString("address") + " at " + smsObj.getString("received_at") + ": \"" + smsObj.getString("body") + "\"");

		} else {
			// In this case, the message arrived from the API SMS address, so we attempt to parse it
			String msgFull = smsObj.getString("body");
			if (msgFull.contains("|")) {
				String[] msgParts = msgFull.split("\\|");
				if (msgParts[0].equalsIgnoreCase(app.rfcxGuardianIdentity.getGuid())) {
					if (msgParts[1].equalsIgnoreCase("in")) {

						JSONObject instrObj = new JSONObject();
						instrObj.put("guid",msgParts[2]);
						instrObj.put("type",msgParts[3]);
						instrObj.put("cmd",msgParts[4]);
						instrObj.put("meta",msgParts[5]);
						instrObj.put("at",msgParts[6]);
						instrObj.put("protocol","sms");

						JSONArray jsonArr = new JSONArray();
						jsonArr.put(instrObj);

						JSONObject instrSendArr = (new JSONObject()).put("instructions",jsonArr);

						// This message is an instruction
						Cursor sendInstr = app.getApplicationContext().getContentResolver().query(
							RfcxComm.getUri("guardian", "instructions", instrSendArr.toString()),
							RfcxComm.getProjection("guardian", "instructions"),
							null, null, null);
						sendInstr.close();

					}
				}
			}
		}

	}

	// Scheduling Tools

	public static boolean addScheduledSmsToQueue(long sendAtOrAfter, String sendTo, String msgBody, Context context) {
		boolean isQueued = false;
		if ((sendTo != null) && (msgBody != null) && !sendTo.equalsIgnoreCase("") && !msgBody.equalsIgnoreCase("")) {
			RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
			String msgId = DeviceSmsUtils.generateMessageId();
			app.smsMessageDb.dbSmsQueued.insert(sendAtOrAfter, sendTo, msgBody, msgId);
			Log.w(logTag, "SMS Queued (ID " + msgId + "): To " + sendTo + " at " + DateTimeUtils.getDateTime(sendAtOrAfter) + ": \"" + msgBody + "\"");
			app.rfcxServiceHandler.triggerService("SmsDispatch", false);
		}
		return isQueued;
	}

	public static boolean addImmediateSmsToQueue(String sendTo, String msgBody, Context context) {
		return addScheduledSmsToQueue(System.currentTimeMillis(), sendTo, msgBody, context);
	}


}
