package org.rfcx.guardian.admin.comms.sbd;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.device.hardware.DeviceHardware_OrangePi_3G_IOT;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SbdUtils {

	public SbdUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SbdUtils");

	RfcxGuardian app;
	private static final int baudRate = 19200;
	private String busyBoxBin = null;
	private String ttyPath = null;

	public static final long sendCmdTimeout = 70000;
	public static final long prepCmdTimeout = 2500;

	public boolean isInFlight = false;
	public int consecutiveDeliveryFailureCount = 0;
	public static final int powerCycleAfterThisManyConsecutiveDeliveryFailures = 5;

	public void init(String ttyPath, String busyBoxBin) {
		this.ttyPath = ttyPath;
		this.busyBoxBin = busyBoxBin;
	}

	public void setupSbdUtils() {
		app.deviceGpioUtils.runGpioCommand("DOUT", "voltage_refr", true);
		setPower(true);
		setPower(false);
	}

	public boolean sendSbdMessage(String msgStr) {

		String errorMsg = "SBD Message was NOT successfully delivered.";
		isInFlight = false;

		try {

			if ((busyBoxBin == null) || !FileUtils.exists(busyBoxBin)) {
				errorMsg = "BusyBox binary not found on system";
			} else if (!isNetworkAvailable()) {
				errorMsg = "No Iridium network currently available";
			} else {
				String[] atCmdSeq = new String[]{ "AT&K0", "AT+SBDD0", "AT+SBDWT=" + msgStr, "AT+SBDIX" };
				Log.d(logTag, DateTimeUtils.getDateTime() + " - Attempting AT Command Sequence: " + TextUtils.join(", ", atCmdSeq));
				isInFlight = true;
				app.rfcxSvc.triggerService(SbdDispatchTimeoutService.SERVICE_NAME, true);
				long atCmdLaunchedAt = System.currentTimeMillis();
				List<String> atCmdResponseLines = ShellCommands.executeCommandAsRoot(atCmdExecStr(ttyPath, busyBoxBin, atCmdSeq));
				isInFlight = false;
				for (String atCmdResponseLine : atCmdResponseLines) {
					if (atCmdResponseLine.contains("+SBDIX:")) {
						if (Integer.parseInt(atCmdResponseLine.substring(atCmdResponseLine.indexOf(":") + 1, atCmdResponseLine.indexOf(","))) <= 2) {
							Log.i(logTag, DateTimeUtils.getDateTime() + " - SBD Message was successfully transmitted ("+DateTimeUtils.timeStampDifferenceFromNowAsReadableString(atCmdLaunchedAt)+").");
							return true;
						}
					}
				}
				errorMsg += " ("+DateTimeUtils.timeStampDifferenceFromNowAsReadableString(atCmdLaunchedAt)+") AT Response: " + TextUtils.join(", ", atCmdResponseLines);
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

		isInFlight = false;
		Log.e(logTag, errorMsg);
		return false;
	}

	private static String atCmdExecStr(String ttyPath, String busyBoxBin, String[] execSteps) {
		StringBuilder execFull = new StringBuilder();

		execFull.append(busyBoxBin).append(" stty -F ").append(ttyPath).append(" ").append(baudRate).append(" cs8 -cstopb -parenb");

		for (int i = 0; i < execSteps.length; i++) {
			long waitMs = (execSteps[i].equalsIgnoreCase("AT+SBDIX")) ? Math.round( sendCmdTimeout * 0.5 ) : prepCmdTimeout;
			execFull.append(" && ")
					.append("echo").append(" -n").append(" '").append(execSteps[i]).append("<br_r>'")
					.append(" | ")
					.append(busyBoxBin).append(" microcom -t ").append(waitMs).append(" -s ").append(baudRate).append(" ").append(ttyPath);
		}
		return execFull.toString();
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



	public int[] findRunningSerialProcessIds() {

		List<Integer> processIds = new ArrayList<>();
		isInFlight = false;

		if (!FileUtils.exists(busyBoxBin)) {
			Log.e(logTag, "Could not run findRunningSerialProcessIds(). BusyBox binary not found on system.");
		} else {
			List<String> processScan = ShellCommands.executeCommandAsRoot(busyBoxBin + " ps -ef | grep "+ ttyPath);

			for (String scanRtrn : processScan) {
				if ((scanRtrn.contains("microcom")) || (scanRtrn.contains("stty"))) {
					String processId = scanRtrn.substring(0, scanRtrn.indexOf("root"));
					processIds.add(Integer.parseInt(processId));
				}
			}
		}

		return ArrayUtils.ListToIntArray(processIds);
	}



	public void setPower(boolean setToOn) {
		app.deviceGpioUtils.runGpioCommand("DOUT", "satellite_power", setToOn);
	}

	public boolean isPowerOn() {
		return app.deviceGpioUtils.readGpioValue("satellite_power", "DOUT");
	}

	public boolean isNetworkAvailable() {
		return app.deviceGpioUtils.readGpioValue("satellite_state", "DOUT");
	}


	// Scheduling Tools

	public static boolean addScheduledSbdToQueue(long sendAtOrAfter, String msgPayload, Context context, boolean triggerDispatchService) {

		boolean isQueued = false;

		if ((msgPayload != null) && !msgPayload.equalsIgnoreCase("")) {

			RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

			String msgId = DeviceSmsUtils.generateMessageId();

			app.sbdMessageDb.dbSbdQueued.insert(sendAtOrAfter, "", msgPayload, msgId);

			if (triggerDispatchService) { app.rfcxSvc.triggerService( SbdDispatchService.SERVICE_NAME, false); }
		}
		return isQueued;
	}

	public static boolean addImmediateSbdToQueue(String msgPayload, Context context) {
		return addScheduledSbdToQueue(System.currentTimeMillis(), msgPayload, context, true);
	}

	public boolean isSatelliteAllowedAtThisTimeOfDay() {
		for (String offHoursRange : TextUtils.split(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_OFF_HOURS), ",")) {
			String[] offHours = TextUtils.split(offHoursRange, "-");
			if (DateTimeUtils.isTimeStampWithinTimeRange(new Date(), offHours[0], offHours[1])) {
				return false;
			}
		}
		return true;
	}

}
