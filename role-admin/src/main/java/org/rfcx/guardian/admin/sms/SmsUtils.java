package org.rfcx.guardian.admin.sms;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SmsUtils {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SmsUtils");


	public static boolean testSmsQueue(String address, long delayInterval, int smsCount, Context context) {

		long thisTime = System.currentTimeMillis();

		for (int i = 1; i <= smsCount; i++) {
			addScheduledSmsToQueue(thisTime, address, i+") SMS Test Message: " + DateTimeUtils.getDateTime(thisTime), context);
			thisTime = thisTime + (delayInterval*1000);
		}

		return true;
	}

	// Incoming Message Tools

	public static void processIncomingSms(String fromAddress, String msgBody, Context context) {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		String apiSmsAddress = app.rfcxPrefs.getPrefAsString("api_sms_address");

	}

	// Scheduling Tools

	public static boolean addScheduledSmsToQueue(long sendAtOrAfter, String sendTo, String msgBody, Context context) {
		boolean isQueued = false;
		if ((sendTo != null) && (msgBody != null) && !sendTo.equalsIgnoreCase("") && !msgBody.equalsIgnoreCase("")) {
			RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
			String msgId = DeviceSmsUtils.generateMessageId();
			app.smsMessageDb.dbSmsQueued.insert(sendAtOrAfter, sendTo, msgBody, msgId);
			Log.w(logTag, "SMS Queued (ID " + msgId + "): To " + sendTo + " at " + DateTimeUtils.getDateTime(sendAtOrAfter) + ": \"" + msgBody + "\"");
		}
		return isQueued;
	}

	public static boolean addImmediateSmsToQueue(String sendTo, String msgBody, Context context) {
		return addScheduledSmsToQueue(System.currentTimeMillis(), sendTo, msgBody, context);
	}


}
