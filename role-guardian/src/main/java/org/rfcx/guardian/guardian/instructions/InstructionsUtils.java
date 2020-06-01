package org.rfcx.guardian.guardian.instructions;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.Iterator;

public class InstructionsUtils {

	public InstructionsUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstructionsUtils");

	private RfcxGuardian app = null;

	public void processReceivedInstructionJson(String jsonStr) {
		try {
			processReceivedInstructionJson(new JSONObject(jsonStr));
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	public void processReceivedInstructionJson(JSONObject jsonObj) {
		try {
			if (jsonObj.has("instructions")) {
				JSONArray instrArr = jsonObj.getJSONArray("instructions");
				for (int i = 0; i < instrArr.length(); i++) {
					JSONObject instrObj = instrArr.getJSONObject(i);
					if (instrObj.has("guid")) {

						String instrGuid = instrObj.getString("guid");
						String instrType = instrObj.getString("type");
						String instrCmd = instrObj.getString("cmd");

						JSONObject instrMetaObj = new JSONObject();
						if (instrObj.getString("meta").length() > 0) {
							instrMetaObj = new JSONObject(instrObj.getString("meta"));
						}

						long instrExecuteAt = System.currentTimeMillis();
						if (instrObj.getString("at").length() > 0) {
							instrExecuteAt = Long.parseLong(instrObj.getString("at"));
						}

						String protocol = "mqtt";
						if (instrObj.has("protocol")) {
							protocol = instrObj.getString("protocol");
						}

						this.app.instructionsDb.dbQueuedInstructions.findByGuidOrCreate(instrGuid, instrType, instrCmd, instrExecuteAt, instrMetaObj.toString(), protocol);

						Log.i(logTag, "Instruction Received ("+protocol+"): "+instrGuid+", "+instrType+", "+instrCmd+", at "+ DateTimeUtils.getDateTime(instrExecuteAt)+", "+instrMetaObj.toString());

						if (protocol.equalsIgnoreCase("mqtt")) {
							this.app.apiCheckInUtils.sendMqttPing(false, new String[]{"instructions"});

						} else if (protocol.equalsIgnoreCase("sms")) {
							Log.e(logTag, "Send SMS Instruction Response: "+ getSingleInstructionInfoAsSerializedString(instrGuid) );

						}
					}
				}
			}

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}
	}

	public String getSingleInstructionInfoAsSerializedString(String instrGuid) {

		String[] instrInfo = new String[]{
				app.rfcxGuardianIdentity.getGuid(),
				"in",
				"",		// guid
				"",		// received_at
				"",		// executed_at
				"",		// attempts
				""		// response
		};

		for (String[] receivedRow : app.instructionsDb.dbQueuedInstructions.getRowsInOrderOfExecution()) {
			if ((receivedRow[0] != null) && instrGuid.equalsIgnoreCase(receivedRow[1])) {
				instrInfo[2] = receivedRow[1];	// guid
				instrInfo[3] = receivedRow[0];	// received_at
				break;
			}
		}

		for (String[] executedRow : app.instructionsDb.dbExecutedInstructions.getRowsInOrderOfExecution()) {
			if ((executedRow[0] != null) && instrGuid.equalsIgnoreCase(executedRow[1])) {
				instrInfo[2] = executedRow[1];    // guid
				instrInfo[3] = executedRow[7];    // received_at
				instrInfo[4] = executedRow[0];    // executed_at
				instrInfo[5] = executedRow[6];    // attempts
				instrInfo[6] = executedRow[5];    // response
				break;
			}
		}
		return TextUtils.join("|", instrInfo);
	}

	public JSONObject getInstructionsInfoAsJson() {

		JSONObject instrObj = new JSONObject();
		try {

			JSONArray receivedInstrArr = new JSONArray();
			for (String[] receivedRow : app.instructionsDb.dbQueuedInstructions.getRowsInOrderOfExecution()) {
				if (receivedRow[0] != null) {
					JSONObject receivedObj = new JSONObject();
					receivedObj.put("guid", receivedRow[1]);
					receivedObj.put("received_at", receivedRow[0]);
					receivedInstrArr.put(receivedObj);
				}
			}
			instrObj.put("received", receivedInstrArr);

			JSONArray executedInstrArr = new JSONArray();
			for (String[] executedRow : app.instructionsDb.dbExecutedInstructions.getRowsInOrderOfExecution()) {
				if (executedRow[0] != null) {
					JSONObject executedObj = new JSONObject();
					executedObj.put("guid", executedRow[1]);
					executedObj.put("received_at", executedRow[7]);
					executedObj.put("executed_at", executedRow[0]);
					executedObj.put("attempts", executedRow[6]);
					executedObj.put("response", executedRow[5]);
					executedInstrArr.put(executedObj);
				}
			}
			instrObj.put("executed", executedInstrArr);

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return instrObj;
	}


	public String executeInstruction(String instrType, String instrCmd, JSONObject instrMeta) {

		JSONObject responseJson = new JSONObject();

		// Set Pref[s]
		if (instrType.equalsIgnoreCase("set") && instrCmd.equalsIgnoreCase("prefs")) {
			try {
				JSONObject prefsKeysVals = instrMeta;
				Iterator<String> prefsKeys = prefsKeysVals.keys();
				while (prefsKeys.hasNext()) {
					String prefKey = prefsKeys.next();
					if (prefsKeysVals.getString(prefKey) instanceof String) {
						app.setSharedPref(prefKey.toLowerCase(), prefsKeysVals.getString(prefKey).toLowerCase());
					}
				}
			} catch (JSONException e) {
				RfcxLog.logExc(logTag, e);
			}

		// Execute Control Command
		} else if (instrType.equalsIgnoreCase("ctrl")) {

			app.deviceControlUtils.runOrTriggerDeviceControl(instrCmd, app.getApplicationContext().getContentResolver());

		}


		return responseJson.toString();
	}

}
