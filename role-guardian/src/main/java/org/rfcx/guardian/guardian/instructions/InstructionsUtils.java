package org.rfcx.guardian.guardian.instructions;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

				List<String> queuedInstrIds = new ArrayList<>();

				for (int i = 0; i < instrArr.length(); i++) {

					JSONObject instrObj = instrArr.getJSONObject(i);

					if (instrObj.has("id")) {

						String instrId = instrObj.getString("id");

						if (	(this.app.instructionsDb.dbExecuted.getCountById(instrId) == 0)
							&&	(this.app.instructionsDb.dbQueued.getCountById(instrId) == 0)
							) {

							String instrType = instrObj.getString("type");

							String instrCmd = instrObj.getString("cmd");

							JSONObject instrMetaObj = (instrObj.getString("meta").length() > 0) ? new JSONObject(instrObj.getString("meta")) : new JSONObject();

							long instrExecuteAt = ( (instrObj.getString("at").length() > 0) ? Long.parseLong(instrObj.getString("at")) : System.currentTimeMillis() ) + InstructionsCycleService.CYCLE_DURATION;

							this.app.instructionsDb.dbQueued.findByIdOrCreate(instrId, instrType, instrCmd, instrExecuteAt, instrMetaObj.toString());

							Log.i(logTag, "Instruction Received with ID '" + instrId + "', Type: '" + instrType + "', Command: '" + instrCmd + "', Send at " + DateTimeUtils.getDateTime(instrExecuteAt) + ", JSON Meta: '" + instrMetaObj.toString() + "'");

							queuedInstrIds.add(instrId);
						}
					}
				}

				// confirm receipt
				if (queuedInstrIds.size() > 0) {
					this.app.apiPingUtils.sendPing(false, new String[]{"instructions"}, true);
				}

			}

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}
	}

	public JSONObject getInstructionsInfoAsJson() {

		JSONObject instrObj = new JSONObject();
		try {

			JSONArray receivedInstrArr = new JSONArray();
			for (String[] receivedRow : app.instructionsDb.dbQueued.getRowsInOrderOfExecution()) {
				if (receivedRow[0] != null) {
					JSONObject receivedObj = new JSONObject();
					receivedObj.put("id", receivedRow[1]);
					receivedObj.put("received_at", receivedRow[0]);
					receivedInstrArr.put(receivedObj);
					app.instructionsDb.dbQueued.updateLastAccessedAtById(receivedRow[1]);
				}
			}
			instrObj.put("received", receivedInstrArr);

			JSONArray executedInstrArr = new JSONArray();
			for (String[] executedRow : app.instructionsDb.dbExecuted.getRowsInOrderOfExecution()) {
				if (executedRow[0] != null) {
					JSONObject executedObj = new JSONObject();
					executedObj.put("id", executedRow[1]);
					executedObj.put("received_at", executedRow[7]);
					executedObj.put("executed_at", executedRow[0]);
					executedObj.put("attempts", executedRow[6]);
					executedObj.put("response", executedRow[5]);
					executedInstrArr.put(executedObj);
					app.instructionsDb.dbExecuted.updateLastAccessedAtById(executedRow[1]);
				}
			}
			instrObj.put("executed", executedInstrArr);

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return instrObj;
	}

	public int getInstructionsCount() {
		return app.instructionsDb.dbQueued.getCount() + app.instructionsDb.dbExecuted.getCount();
	}


	public String executeInstruction(String instrType, String instrCmd, JSONObject instrMeta) {

		JSONObject responseJson = new JSONObject();

		try {

			// Set Pref[s]
			if (	instrType.equalsIgnoreCase("set")
				&& 	instrCmd.equalsIgnoreCase("prefs")
			) {

				JSONObject prefsKeysVals = instrMeta;
				Iterator<String> prefsKeys = prefsKeysVals.keys();
				while (prefsKeys.hasNext()) {
					String prefKey = prefsKeys.next();
					if (prefsKeysVals.getString(prefKey) instanceof String) {
						app.setSharedPref(prefKey.toLowerCase(), prefsKeysVals.getString(prefKey).toLowerCase());
					}
				}

			// Execute Control Command
			} else if (instrType.equalsIgnoreCase("ctrl")) {

				app.deviceControlUtils.runOrTriggerDeviceControl(instrCmd, app.getResolver());

			// Execute Send Command
            } else if (instrType.equalsIgnoreCase("send")) {

                if (instrCmd.equalsIgnoreCase("ping")) {

                    JSONArray inclFieldsArr = instrMeta.getJSONArray("include");
                    String[] inclFields = new String[inclFieldsArr.length()];
                    for (int i = 0; i < inclFieldsArr.length(); i++) {
                        inclFields[i] = inclFieldsArr.getString(i).toLowerCase();
                    }
					app.apiPingUtils.sendPing(false, inclFields, true);

                } else if (instrCmd.equalsIgnoreCase("sms")) {

					String sendAt = instrMeta.has("at") ? ""+Long.parseLong(instrMeta.getString("at")) : ""+System.currentTimeMillis();
					String sendTo = instrMeta.has("to") ? instrMeta.getString("to") : app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SMS_ADDRESS);
					String msgBody = instrMeta.has("body") ? instrMeta.getString("body") : "";

					app.apiSmsUtils.queueSmsToSend(sendAt, sendTo, msgBody);

                }

            }

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}


		return responseJson.toString();
	}

}
