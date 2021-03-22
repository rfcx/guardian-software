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

	public SbdUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SbdUtils");

	RfcxGuardian app;
	private static final int sdbBaudRate = 19200;
	private static final String sdbUartPath = "/dev/ttyMT1";


	public void setupSbdUtils() {
		ttySetup();
	}

	public boolean sendSbdMessage(String msgStr) {

		if (!isNetworkAvailable()) {
			Log.e(logTag, "No Iridium network currently available");
		} else {
			ShellCommands.executeCommandAsRootAndIgnoreOutput(atCmdExecStr(new String[]{"AT+SBDWT=FLUSH_MT", "AT&K0", "AT+SBDWT=" + msgStr, "AT+SBDI"}));
		}

		return true;
	}

	private static String atCmdExecStr(String[] execSteps) {
		StringBuilder execFull = new StringBuilder();
		for (int i = 0; i < execSteps.length; i++) {
			int waitMs = 500;
			String joinStr = "; sleep 1; ";
			execFull.append("echo -n '").append(execSteps[i]).append("<br_r>");

			if (i == (execSteps.length-1)) {
				execFull.append("<br_r>");
				waitMs = 5000;
				joinStr = "";
			}

			execFull.append("' | /system/xbin/busybox microcom -t ").append(waitMs).append(" -s ").append(sdbBaudRate).append(" ").append(sdbUartPath).append(joinStr);
		}
		return execFull.toString();
	}

	private void ttySetup() {

		app.deviceGpioUtils.runGpioCommand("DOUT", "voltage_refr", true);
		setPower(true);

		ShellCommands.executeCommandAsRootAndIgnoreOutput(
				"/system/xbin/busybox stty -F " + sdbUartPath + " " + sdbBaudRate + " cs8 -cstopb -parenb"
				+ "; sleep 1; "
				+ atCmdExecStr( new String[] { "AT", "AT+SBDWT=FLUSH_MT" } )
		);
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






	public void setPower(boolean setToOn) {
		app.deviceGpioUtils.runGpioCommand("DOUT", "iridium_power", setToOn);
	}

	public boolean isPowerOn() {
		return app.deviceGpioUtils.readGpioValue("iridium_power", "DOUT");
	}

	public boolean isNetworkAvailable() {
		return app.deviceGpioUtils.readGpioValue("iridium_netav", "DOUT");
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
