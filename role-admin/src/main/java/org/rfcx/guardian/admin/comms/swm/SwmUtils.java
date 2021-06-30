package org.rfcx.guardian.admin.comms.swm;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.comms.sbd.SbdDispatchService;
import org.rfcx.guardian.admin.comms.sbd.SbdDispatchTimeoutService;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;

public class SwmUtils {

	public SwmUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmUtils");

	RfcxGuardian app;
	private static final String busyBoxBin = "/system/xbin/busybox";
	private static final int baudRate = 115200;
	private static final String ttyPath = "/dev/ttyMT1";

	public static final long sendCmdTimeout = 70000;
	public static final long prepCmdTimeout = 2500;

	public boolean isInFlight = false;
	public int consecutiveDeliveryFailureCount = 0;
	public static final int powerCycleAfterThisManyConsecutiveDeliveryFailures = 5;


	public void setupSwmUtils() {
		app.deviceGpioUtils.runGpioCommand("DOUT", "voltage_refr", true);
		setPower(true);
		setPower(false);
	}

	public boolean sendSwmMessage(String msgStr) {

		String errorMsg = "SWM Message was NOT successfully delivered.";
		isInFlight = false;

		try {

			if (!FileUtils.exists(busyBoxBin)) {
				errorMsg = "BusyBox binary not found on system";
			} else if (!isNetworkAvailable()) {
				errorMsg = "No Swarm network currently available";
			} else {
				String[] atCmdSeq = new String[]{ "AT&K0", "AT+SBDD0", "AT+SBDWT=" + msgStr, "AT+SBDIX" };
				Log.d(logTag, DateTimeUtils.getDateTime() + " - Attempting AT Command Sequence: " + TextUtils.join(", ", atCmdSeq));
				isInFlight = true;
				app.rfcxSvc.triggerService(SbdDispatchTimeoutService.SERVICE_NAME, true);
				long atCmdLaunchedAt = System.currentTimeMillis();
				List<String> atCmdResponseLines = ShellCommands.executeCommandAsRoot(atCmdExecStr(atCmdSeq));
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

	private static String atCmdExecStr(String[] execSteps) {
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

	public static void processIncomingSwm(JSONObject smsObj, Context context) throws JSONException {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

		// In this case, the message arrived from the API SMS address, so we attempt to parse it
		Log.w(logTag, "SWM received from API ''.");
		String segmentPayload = smsObj.getString("body");
		Cursor swmSegmentReceivedResponse =
				app.getResolver().query(
						RfcxComm.getUri("guardian", "segment_receive_swm", RfcxComm.urlEncode(segmentPayload)),
						RfcxComm.getProjection("guardian", "segment_receive_swm"),
						null, null, null);

		if (swmSegmentReceivedResponse != null) {
			swmSegmentReceivedResponse.close();
		}

	}



	public int[] findRunningSerialProcessIds() {

		List<Integer> processIds = new ArrayList<>();
		isInFlight = false;

		if (!FileUtils.exists(busyBoxBin)) {
			Log.e(logTag, "Could not run findRunningSerialProcessIds(). BusyBox binary not found on system.");
		} else {
			List<String> processScan = ShellCommands.executeCommandAsRoot(busyBoxBin + " ps -ef | grep /dev/ttyMT");

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

	public static boolean addScheduledSwmToQueue(long sendAtOrAfter, String msgPayload, Context context, boolean triggerDispatchService) {

		boolean isQueued = false;

		if ((msgPayload != null) && !msgPayload.equalsIgnoreCase("")) {

			RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

			String msgId = DeviceSmsUtils.generateMessageId();

			app.sbdMessageDb.dbSbdQueued.insert(sendAtOrAfter, "", msgPayload, msgId);

			if (triggerDispatchService) { app.rfcxSvc.triggerService( SbdDispatchService.SERVICE_NAME, false); }
		}
		return isQueued;
	}

	public static boolean addImmediateSwmToQueue(String msgPayload, Context context) {
		return addScheduledSwmToQueue(System.currentTimeMillis(), msgPayload, context, true);
	}


}
