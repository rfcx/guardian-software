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
	private static final int baudRate = 115200;
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

	public void setupSwmUtils() {
		app.deviceGpioUtils.runGpioCommand("DOUT", "voltage_refr", true);
		setPower(true);
		setPower(false);
	}

	private static String atCmdExecStr(String ttyPath, String busyBoxBin, String[] execSteps) {
		StringBuilder execFull = new StringBuilder();

		execFull.append(busyBoxBin).append(" stty -F ").append(ttyPath).append(" ").append(baudRate).append(" cs8 -cstopb -parenb");

		for (int i = 0; i < execSteps.length; i++) {
			execFull.append(" && ")
					.append("echo").append(" -n").append(" '").append(execSteps[i]).append("'")
					.append(" | ")
					.append(busyBoxBin).append(" microcom -t ").append(prepCmdTimeout).append(" -s ").append(baudRate).append(" ").append(ttyPath);
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

			app.swmMessageDb.dbSwmQueued.insert(sendAtOrAfter, "", msgPayload, msgId);

			if (triggerDispatchService) { app.rfcxSvc.triggerService( SwmDispatchService.SERVICE_NAME, false); }
		}
		return isQueued;
	}

	public static boolean addImmediateSwmToQueue(String msgPayload, Context context) {
		return addScheduledSwmToQueue(System.currentTimeMillis(), msgPayload, context, true);
	}

	private static String getNMEAChecksum(String in) {
		int checksum = 0;
		if (in.startsWith("$")) {
			in = in.substring(1);
		}

		int end = in.indexOf('*');
		if (end == -1)
			end = in.length();
		for (int i = 0; i < end; i++) {
			checksum = checksum ^ in.charAt(i);
		}
		String hex = Integer.toHexString(checksum);
		if (hex.length() == 1)
			hex = "0" + hex;
		return hex.toUpperCase();

	}

	public boolean sendSwmMessage(String msgStr) {

		String errorMsg = "SWM Message was NOT successfully delivered.";

		try {

			if (!FileUtils.exists(busyBoxBin)) {
				errorMsg = "BusyBox binary not found on system";
			} else {
				String command = "TD " + msgStr;
				String[] atCmdSeq = new String[]{ "$" + command + "*" + getNMEAChecksum(command) };
				Log.d(logTag, DateTimeUtils.getDateTime() + " - Attempting TD Command Sequence: " + TextUtils.join(", ", atCmdSeq));
				app.rfcxSvc.triggerService(SwmDispatchService.SERVICE_NAME, true);
				List<String> atCmdResponseLines = ShellCommands.executeCommandAsRoot(atCmdExecStr(ttyPath, busyBoxBin, atCmdSeq));
				for (String atCmdResponseLine : atCmdResponseLines) {
					if (atCmdResponseLine.contains("OK")) {
						Log.i(logTag, DateTimeUtils.getDateTime() + " - SWM Sending Message was successfully transmitted ");
						return true;
					} else {
						errorMsg += " TD Response: " + TextUtils.join(", ", atCmdResponseLines);
					}
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

		Log.e(logTag, errorMsg);
		return false;
	}

	private boolean updateQueueMessagesFromSwarm() {
        String errorMsg = "SWM Sleep Command was NOT successfully delivered.";

        try {
            List<String[]> guardianMessageIdQueues = app.swmMessageDb.dbSwmQueued.getAllRows();
            ArrayList<String> swarmMessageIdQueues = new ArrayList<>();
            String command = "MT L=U";
            String[] atCmdSeq = new String[]{ "$" + command + "*" + getNMEAChecksum(command) };
            Log.d(logTag, DateTimeUtils.getDateTime() + " - Attempting Query Unsent Message Command : " + TextUtils.join(", ", atCmdSeq));
            List<String> atCmdResponseLines = ShellCommands.executeCommandAsRoot(atCmdExecStr(ttyPath, busyBoxBin, atCmdSeq));
            for (String atCmdResponseLine : atCmdResponseLines) {
            	//                            hexdecimal data         message id   timestamp
            	// Example message : $MT 68692066726f6d20737761726d,4428826476689,1605639598*55
				swarmMessageIdQueues.add(atCmdResponseLine.split(",")[1]);
            }
            for (String[] guardianMessage: guardianMessageIdQueues) {
            	if (!swarmMessageIdQueues.contains(guardianMessage[4])) {
            		app.swmMessageDb.dbSwmSent.insert(Long.parseLong(guardianMessage[1]), guardianMessage[2], guardianMessage[3], guardianMessage[4]);
            		app.swmMessageDb.dbSwmQueued.deleteSingleRowByMessageId(guardianMessage[4]);
				}
			}
            return true;
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }

        Log.e(logTag, errorMsg);
        return false;
    }

	private boolean setSleep(long time) {
        String errorMsg = "SWM Sleep Command was NOT successfully delivered.";
		try {
		    String command = "SL S=" + time;
		    String[] atCmdSeq = new String[]{ "$" + command + "*" + getNMEAChecksum(command) };
		    Log.d(logTag, DateTimeUtils.getDateTime() + " - Attempting Sleep Command : " + TextUtils.join(", ", atCmdSeq));
		    List<String> atCmdResponseLines = ShellCommands.executeCommandAsRoot(atCmdExecStr(ttyPath, busyBoxBin, atCmdSeq));
		    for (String atCmdResponseLine : atCmdResponseLines) {
		        if (atCmdResponseLine.contains("OK")) {
		            Log.i(logTag, DateTimeUtils.getDateTime() + " - Sleep Command was successfully delivered ");
		            return true;
		        } else if (atCmdResponseLine.contains("WAKE")) {
		            errorMsg = "Swarm has woken from sleep mode (getting message while sleeping)";
                }
		    }
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
        Log.e(logTag, errorMsg);
		return false;
	}




}
