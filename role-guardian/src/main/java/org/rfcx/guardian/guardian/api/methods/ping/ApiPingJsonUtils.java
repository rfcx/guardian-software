package org.rfcx.guardian.guardian.api.methods.ping;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInJsonUtils;
import org.rfcx.guardian.utility.device.hardware.DeviceHardwareUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.io.IOException;

public class ApiPingJsonUtils {

	public ApiPingJsonUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiPingJsonUtils");

	private RfcxGuardian app;

	public String buildPingJson(boolean includeAllExtraFields, String[] includeExtraFields) throws JSONException, IOException {

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
			pingObj.put("instructions", app.instructionsUtils.getInstructionsInfoAsJson());
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "prefs")) {
			pingObj.put("prefs", app.apiCheckInJsonUtils.buildCheckInPrefsJsonObj());
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
			String sentinelPower = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "sentinel_power", app.getApplicationContext().getContentResolver()));
			if (sentinelPower.length() > 0) { pingObj.put("sentinel_power", sentinelPower); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_sensor")) {
			String sentinelSensor = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "sentinel_sensor", app.getApplicationContext().getContentResolver()));
			if (sentinelSensor.length() > 0) { pingObj.put("sentinel_sensor", sentinelSensor); }
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "storage")) {
			String systemStorage = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "system_storage", app.getApplicationContext().getContentResolver()));
			if (systemStorage.length() > 0) { pingObj.put("storage", systemStorage); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "memory")) {
			String systemMemory = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "system_memory", app.getApplicationContext().getContentResolver()));
			if (systemMemory.length() > 0) { pingObj.put("memory", systemMemory); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "cpu")) {
			String systemCPU = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "system_cpu", app.getApplicationContext().getContentResolver()));
			if (systemCPU.length() > 0) { pingObj.put("cpu", systemCPU); }
			includeMeasuredAt = true;
		}

		if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "network")) {
			String networkCPU = ApiCheckInJsonUtils.getConcatMetaField(RfcxComm.getQueryContentProvider("admin", "get_momentary_values", "system_network", app.getApplicationContext().getContentResolver()));
			if (networkCPU.length() > 0) { pingObj.put("network", networkCPU); }
			includeMeasuredAt = true;
		}

		if (includeMeasuredAt) { pingObj.put("measured_at", System.currentTimeMillis()); }

		Log.d(logTag, pingObj.toString());

//		app.apiSegmentUtils.constructSegmentsGroupForQueue("png", "sms", pingObj.toString(), null);

		JSONObject guardianObj = new JSONObject();
		guardianObj.put("guid", app.rfcxGuardianIdentity.getGuid());
		guardianObj.put("token", app.rfcxGuardianIdentity.getAuthToken());
		pingObj.put("guardian", guardianObj);

		return pingObj.toString();
	}





}
