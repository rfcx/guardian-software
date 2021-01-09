package org.rfcx.guardian.guardian.api.methods.ping;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInJsonUtils;
import org.rfcx.guardian.utility.device.hardware.DeviceHardwareUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

public class ApiPingJsonUtils {

	public ApiPingJsonUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiPingJsonUtils");

	private RfcxGuardian app;

	public String buildPingJson(boolean includeAllExtraFields, String[] includeExtraFields) throws JSONException {

		JSONObject pingObj = new JSONObject();

		boolean includeMeasuredAt = false;

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "battery")) {
			pingObj.put("battery", app.deviceBattery.getBatteryStateAsConcatString(app.getApplicationContext(), null) );
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "checkins")) {
			pingObj.put("checkins", app.apiCheckInJsonUtils.getCheckInStatusInfoForJson(new String[] {}));
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "instructions")) {
			if (app.instructionsUtils.getInstructionsCount() > 0) {
				pingObj.put("instructions", app.instructionsUtils.getInstructionsInfoAsJson());
			}
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "prefs")) {
			pingObj.put("prefs", app.apiCheckInJsonUtils.buildCheckInPrefsJsonObj());
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "purged")) {
			pingObj.put("purged", app.assetUtils.getAssetExchangeLogList("purged", 4 * app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_META_SEND_BUNDLE_LIMIT)));
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sms")) {
			JSONArray smsArr = RfcxComm.getQuery("admin", "database_get_all_rows", "sms", app.getResolver());
			if (smsArr.length() > 0) { pingObj.put("messages", smsArr); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "device")) {
			JSONObject deviceJsonObj = new JSONObject();
			deviceJsonObj.put("phone", app.deviceMobilePhone.getMobilePhoneInfoJson());
			deviceJsonObj.put("android", DeviceHardwareUtils.getInfoAsJson());
			pingObj.put("device", deviceJsonObj);
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "software")) {
			pingObj.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_power")) {
			String sentinelPower = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "sentinel_power", app.getResolver()));
			if (sentinelPower.length() > 0) { pingObj.put("sentinel_power", sentinelPower); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_sensor")) {
			String sentinelSensor = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "sentinel_sensor", app.getResolver()));
			if (sentinelSensor.length() > 0) { pingObj.put("sentinel_sensor", sentinelSensor); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "storage")) {
			String systemStorage = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_storage", app.getResolver()));
			if (systemStorage.length() > 0) { pingObj.put("storage", systemStorage); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "memory")) {
			String systemMemory = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_memory", app.getResolver()));
			if (systemMemory.length() > 0) { pingObj.put("memory", systemMemory); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "cpu")) {
			String systemCPU = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_cpu", app.getResolver()));
			if (systemCPU.length() > 0) { pingObj.put("cpu", systemCPU); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "network")) {
			String systemNetwork = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_network", app.getResolver()));
			if (systemNetwork.length() > 0) { pingObj.put("network", systemNetwork); }
			includeMeasuredAt = true;
		}

		if (includeMeasuredAt) { pingObj.put("measured_at", System.currentTimeMillis()); }

		Log.d(logTag, pingObj.toString());

		return pingObj.toString();
	}


	public String injectGuardianIdentityIntoJson(String jsonBlobStr) {

		String outputJsonStr = jsonBlobStr;

		try {

			JSONObject jsonObj = new JSONObject(jsonBlobStr);
			JSONObject guardianObj = new JSONObject();
			guardianObj.put("guid", app.rfcxGuardianIdentity.getGuid());
			guardianObj.put("token", app.rfcxGuardianIdentity.getAuthToken());
			jsonObj.put("guardian", guardianObj);
			outputJsonStr = jsonObj.toString();

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}

		return outputJsonStr;
	}



}
