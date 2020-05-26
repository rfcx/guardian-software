package org.rfcx.guardian.guardian.instructions;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class InstructionsUtils {

	public InstructionsUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstructionsUtils");

	private RfcxGuardian app = null;

	public void processInstructionJson(String jsonStr) {
		try {
			processInstructionJson(new JSONObject(jsonStr));
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	public void processInstructionJson(JSONObject jsonObj) {
		try {
			if (jsonObj.has("instructions")) {
				JSONArray instrArr = jsonObj.getJSONArray("instructions");
				for (int i = 0; i < instrArr.length(); i++) {
					JSONObject instrObj = instrArr.getJSONObject(i);
					if (instrObj.has("guid")) {
						String instrGuid = instrObj.getString("guid");
						String instrType = instrObj.getString("type");
						String instrCommand = instrObj.getString("command");
						JSONObject instrMetaObj = instrObj.getJSONObject("meta");
						long instrExecuteAt = Long.parseLong(instrObj.getString("execute_at"));

						this.app.instructionsDb.dbQueuedInstructions.findByGuidOrCreate(instrGuid, instrType, instrCommand, ((instrExecuteAt == 0) ? System.currentTimeMillis() : instrExecuteAt ), instrMetaObj.toString());

//						Log.i(logTag, "Instruction Received: Guid: "+instrGuid+", "+instrType+", "+instrCommand+", at "+ DateTimeUtils.getDateTime(instrExecuteAt)+", "+instrMetaObj.toString());


					}
				}
			}


		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}
	}

}
