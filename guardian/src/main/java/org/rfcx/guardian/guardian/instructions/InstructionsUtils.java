package org.rfcx.guardian.guardian.instructions;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class InstructionsUtils {

	public InstructionsUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstructionsUtils");

	private RfcxGuardian app = null;

	public void processInstructionMessage(byte[] messagePayload) {

		String jsonStr = StringUtils.UnGZipByteArrayToString(messagePayload);
		Log.i(logTag, "Instructions: " + jsonStr);

		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			if (jsonObj.has("instructions")) {
				JSONArray instructionsArr = jsonObj.getJSONArray("instructions");
				for (int i = 0; i < instructionsArr.length(); i++) {
					JSONObject instrObj = instructionsArr.getJSONObject(i);

					if (instrObj.has("id")) {
						String instrId = instrObj.getString("id");
						String instrType = instrObj.getString("type");
						String instrCommand = instrObj.getString("command");
						long instrExecuteAt = (long) Long.parseLong(instrObj.getString("execute_at"));
						JSONObject instrMetaObj = instrObj.getJSONObject("meta");



					}
				}
			}


		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);

		}

	}

}
