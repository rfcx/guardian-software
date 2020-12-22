package org.rfcx.guardian.satellite.sbd;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.satellite.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SbdUtils {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SbdUtils");


	public static boolean testSbdQueue(String address, long delayInterval, int smsCount, Context context) {
		return sendSbd(address, "", delayInterval, smsCount, context);
	}

	// with user's defined message
	public static boolean processSendingSbd(String address, String message, long delayInterval, int smsCount, Context context) {
		return sendSbd(address, message, delayInterval, smsCount, context);
	}

	private static boolean sendSbd(String address, String message, long delayInterval, int smsCount, Context context) {
		long thisTime = System.currentTimeMillis();

		for (int i = 1; i <= smsCount; i++) {
			addScheduledSbdToQueue(thisTime, address, i+") SBD Test Message: " + ((!message.isEmpty()) ? message : DateTimeUtils.getDateTime(thisTime)), context, true);
			thisTime = thisTime + (delayInterval*1000);
		}

		return true;
	}

	// Incoming Message Tools

	public static void processIncomingSbd(JSONObject smsObj, Context context) throws JSONException {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

		Log.w(logTag, "SBD received...");
		String segmentPayload = smsObj.getString("body");
		Cursor smsSegmentReceivedContentProviderResponse =
				app.getApplicationContext().getContentResolver().query(
						RfcxComm.getUri("guardian", "segment_receive_sbd", RfcxComm.urlEncode(segmentPayload)),
						RfcxComm.getProjection("guardian", "segment_receive_sbd"),
						null, null, null);
		smsSegmentReceivedContentProviderResponse.close();

	}

	// Scheduling Tools

	public static boolean addScheduledSbdToQueue(long sendAtOrAfter, String sendTo, String msgBody, Context context, boolean triggerDispatchService) {

		boolean isQueued = false;

		if ((sendTo != null) && (msgBody != null) && !sendTo.equalsIgnoreCase("") && !msgBody.equalsIgnoreCase("")) {

			RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

			String msgId = DeviceSmsUtils.generateMessageId();

			app.sbdMessageDb.dbSbdQueued.insert(sendAtOrAfter, sendTo, msgBody, msgId);

			Log.w(logTag, "SBD Queued (ID " + msgId + "): To " + sendTo + " at " + DateTimeUtils.getDateTime(sendAtOrAfter) + ": \"" + msgBody + "\"");

			if (triggerDispatchService) { app.rfcxServiceHandler.triggerService("SbdDispatch", false); }
		}
		return isQueued;
	}

	public static boolean addImmediateSbdToQueue(String sendTo, String msgBody, Context context) {
		return addScheduledSbdToQueue(System.currentTimeMillis(), sendTo, msgBody, context, true);
	}


}
