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

import java.util.Date;

public class ApiPingJsonUtils {

	public ApiPingJsonUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiPingJsonUtils");

	private RfcxGuardian app;

	public String buildPingJson(boolean includeAllExtraFields, String[] includeExtraFields, int includeMetaJsonBundles) throws JSONException {

		JSONObject jsonObj = new JSONObject();

		boolean includeMeasuredAt = false;

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "battery")) {
			jsonObj.put("battery", app.deviceBattery.getBatteryStateAsConcatString(app.getApplicationContext(), null) );
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "checkins")) {
			jsonObj.put("checkins", app.apiCheckInJsonUtils.getCheckInStatusInfoForJson(new String[] {}));
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "instructions")) {
			if (app.instructionsUtils.getInstructionsCount() > 0) {
				jsonObj.put("instructions", app.instructionsUtils.getInstructionsInfoAsJson());
			}
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "library")) {
			if (app.assetLibraryUtils.getLibraryAssetCount() > 0) {
				jsonObj.put("library", app.assetLibraryUtils.getLibraryInfoAsJson());
			}
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "detections")) {
			if (app.audioDetectionDb.dbFiltered.getCount() > 0) {
				jsonObj.put("detections", app.audioDetectionDb.dbFiltered.getSimplifiedConcatRows());
				app.audioDetectionDb.dbFiltered.clearRowsBefore(new Date(System.currentTimeMillis()));
			}
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "prefs")) {
			jsonObj.put("prefs", app.apiCheckInJsonUtils.buildCheckInPrefsJsonObj(true));
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "purged")) {
			jsonObj.put("purged", app.assetUtils.getAssetExchangeLogList("purged", 4 * app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_META_SEND_BUNDLE_LIMIT)));
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sms")) {
			JSONArray smsArr = RfcxComm.getQuery("admin", "database_get_all_rows", "sms", app.getResolver());
			if (smsArr.length() > 0) { jsonObj.put("messages", smsArr); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "device")) {
			JSONObject deviceJsonObj = new JSONObject();
			deviceJsonObj.put("phone", app.deviceMobilePhone.getMobilePhoneInfoJson());
			deviceJsonObj.put("android", DeviceHardwareUtils.getInfoAsJson());
			deviceJsonObj.put("hardware", DeviceHardwareUtils.getInfoAsJson());
			jsonObj.put("device", deviceJsonObj);
		}

//		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "guardian")) {
//			JSONObject guardianJsonObj = new JSONObject();
//			guardianJsonObj.put("is_registered", app.isGuardianRegistered());
//			jsonObj.put("guardian", guardianJsonObj);
//		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "software")) {
			jsonObj.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_power")) {
			String sentinelPower = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "sentinel_power", app.getResolver()));
			if (sentinelPower.length() > 0) { jsonObj.put("sentinel_power", sentinelPower); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_sensor")) {
			String sentinelSensor = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "sentinel_sensor", app.getResolver()));
			if (sentinelSensor.length() > 0) { jsonObj.put("sentinel_sensor", sentinelSensor); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "storage")) {
			String systemStorage = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_storage", app.getResolver()));
			if (systemStorage.length() > 0) { jsonObj.put("storage", systemStorage); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "memory")) {
			String systemMemory = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_memory", app.getResolver()));
			if (systemMemory.length() > 0) { jsonObj.put("memory", systemMemory); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "cpu")) {
			String systemCPU = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_cpu", app.getResolver()));
			if (systemCPU.length() > 0) { jsonObj.put("cpu", systemCPU); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "network")) {
			String systemNetwork = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_network", app.getResolver()));
			if (systemNetwork.length() > 0) { jsonObj.put("network", systemNetwork); }
			includeMeasuredAt = true;
		}

		if (includeMeasuredAt) { jsonObj.put("measured_at", System.currentTimeMillis()); }

		if ((includeAllExtraFields && (includeMetaJsonBundles > 0)) || ArrayUtils.doesStringArrayContainString(includeExtraFields, "meta")) {

			jsonObj = app.apiCheckInJsonUtils.retrieveAndBundleMetaJson(jsonObj, Math.max(includeMetaJsonBundles, 1), false);
		}

		int limitLogsTo = 1500;
		String strLogs = jsonObj.toString();
		Log.d(logTag, (strLogs.length() <= limitLogsTo) ? strLogs : strLogs.substring(0, limitLogsTo) + "...");

		return jsonObj.toString();
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
