package org.rfcx.guardian.admin.sbd;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.sms.SmsDispatchService;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SbdUtils {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SbdUtils");


	private static final int sdbBaudRate = 19200;
	private static final String sdbUartPath = "/dev/ttyMT1";



	//echo 'AT&K0\r\n' | busybox microcom -t 3000 -s 19200 /dev/ttyMT1 && echo 'AT+SBDWT=FLUSH_MT\r\n' | busybox microcom -t 3000 -s 19200 /dev/ttyMT1 && echo 'AT+SBDWT=1234\r\n' | busybox microcom -t 3000 -s 19200 /dev/ttyMT1 && echo 'AT+SBDIX\r' | busybox microcom -t 3000 -s 19200 /dev/ttyMT1



//	public void setPower(boolean setToOn) {
//		app.deviceGpioUtils.runGpioCommand("DOUT", "iridium_power", !setToOn);
//	}


//	public static boolean testSmsQueue(String address, long delayInterval, int smsCount, Context context) {
//		return sendSms(address, "", delayInterval, smsCount, context);
//	}
//
//	// with user's defined message
//	public static boolean processSendingSms(String address, String message, long delayInterval, int smsCount, Context context) {
//		return sendSms(address, message, delayInterval, smsCount, context);
//	}

	public static boolean sendSbdMessage(String message) {
		List<String> execSteps = new ArrayList<>();
		execSteps.add(atCmdExecStr("AT&K0", 3000));
		execSteps.add(atCmdExecStr("AT+SBDWT=FLUSH_MT", 3000));
		execSteps.add(atCmdExecStr("AT+SBDWT="+message, 3000));
		execSteps.add(atCmdExecStr("AT+SBDIX", 10000));
		execSteps.add(atCmdExecStr("AT&K0", 3000));
		ShellCommands.executeCommandAsRootAndIgnoreOutput(TextUtils.join("; ", execSteps));
		return true;
	}

	private static String atCmdExecStr(String cmdStr, int waitMs) {
		return "echo -n '" + cmdStr + "<br_r>" + "' | /system/xbin/busybox microcom -t " + waitMs + " -s " + sdbBaudRate + " " + sdbUartPath;
	}

	// Incoming Message Tools

	public static void processIncomingSbd(JSONObject smsObj, Context context) throws JSONException {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

		// In this case, the message arrived from the API SMS address, so we attempt to parse it
		Log.w(logTag, "SBD received from API ''.");
		String segmentPayload = smsObj.getString("body");
		Cursor sbdSegmentReceivedResponse =
				app.getResolver().query(
						RfcxComm.getUri("guardian", "segment_receive_sbd", RfcxComm.urlEncode(segmentPayload)),
						RfcxComm.getProjection("guardian", "segment_receive_sbd"),
						null, null, null);

		if (sbdSegmentReceivedResponse != null) {
			sbdSegmentReceivedResponse.close();
		}

	}

	// Scheduling Tools

//	public static boolean addScheduledSmsToQueue(long sendAtOrAfter, String sendTo, String msgBody, Context context, boolean triggerDispatchService) {
//
//		boolean isQueued = false;
//
//		if ((sendTo != null) && (msgBody != null) && !sendTo.equalsIgnoreCase("") && !msgBody.equalsIgnoreCase("")) {
//
//			RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
//
//			String msgId = DeviceSmsUtils.generateMessageId();
//
//			app.smsMessageDb.dbSmsQueued.insert(sendAtOrAfter, sendTo, msgBody, msgId);
//
//			if (!sendTo.equalsIgnoreCase(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SMS_ADDRESS))) {
//				Log.w(logTag, "SMS Queued (ID " + msgId + "): To " + sendTo + " at " + DateTimeUtils.getDateTime(sendAtOrAfter) + ": \"" + msgBody + "\"");
//			}
//
//			if (triggerDispatchService) { app.rfcxServiceHandler.triggerService( SmsDispatchService.SERVICE_NAME, false); }
//		}
//		return isQueued;
//	}
//
//	public static boolean addImmediateSmsToQueue(String sendTo, String msgBody, Context context) {
//		return addScheduledSmsToQueue(System.currentTimeMillis(), sendTo, msgBody, context, true);
//	}


}
