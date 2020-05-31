package org.rfcx.guardian.guardian.instructions;

import android.content.Context;
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

						this.app.instructionsDb.dbQueuedInstructions.findByGuidOrCreate(instrGuid, instrType, instrCmd, instrExecuteAt, instrMetaObj.toString());

						Log.i(logTag, "Instruction Received: Guid: "+instrGuid+", "+instrType+", "+instrCmd+", at "+ DateTimeUtils.getDateTime(instrExecuteAt)+", "+instrMetaObj.toString());

					}
				}
			}


		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}
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
