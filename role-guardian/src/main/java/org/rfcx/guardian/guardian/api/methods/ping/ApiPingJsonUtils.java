package org.rfcx.guardian.guardian.api.methods.ping;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.asset.meta.MetaJsonUtils;
import org.rfcx.guardian.utility.device.hardware.DeviceHardwareUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

public class ApiPingJsonUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiPingJsonUtils");
    private final RfcxGuardian app;

    public ApiPingJsonUtils(Context context) {

        this.app = (RfcxGuardian) context.getApplicationContext();

    }

    public String buildPingJson(boolean includeAllExtraFields, String[] includeExtraFields, int includeAssetBundleCount, boolean printJsonToLogs, String[] excludeFieldsFromLogs, boolean shouldShorten) throws JSONException {

        JSONObject jsonObj = new JSONObject();

        boolean includePurgedAssetList = false;

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "battery")) {
            jsonObj.put("battery", app.deviceBattery.getBatteryStateAsConcatString(app.getApplicationContext(), null));
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

        if (ArrayUtils.doesStringArrayContainString(includeExtraFields, "prefs_full")) {
            jsonObj.put("prefs", app.metaJsonUtils.buildPrefsJsonObj(true, true));
        } else if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "prefs")) {
            jsonObj.put("prefs", app.metaJsonUtils.buildPrefsJsonObj(true, false));
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sms")) {
            JSONArray smsArr = RfcxComm.getQuery("admin", "database_get_all_rows", "sms", app.getResolver());
            if (smsArr.length() > 0) {
                jsonObj.put("messages", smsArr);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "device")) {
            JSONObject deviceJsonObj = new JSONObject();
            deviceJsonObj.put("phone", app.deviceMobilePhone.getMobilePhoneInfoJson());
            deviceJsonObj.put("android", DeviceHardwareUtils.getInfoAsJson());
            jsonObj.put("device", deviceJsonObj);
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "software")) {
            jsonObj.put("software", TextUtils.join("|", RfcxRole.getInstalledRoleVersions(RfcxGuardian.APP_ROLE, app.getApplicationContext())));
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_power")) {
            String sentinelPower = MetaJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "sentinel_power", app.getResolver()));
            if (sentinelPower.length() > 0) {
                jsonObj.put("sentinel_power", sentinelPower);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_sensor")) {
            String sentinelSensor = MetaJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "sentinel_sensor", app.getResolver()));
            if (sentinelSensor.length() > 0) {
                jsonObj.put("sentinel_sensor", sentinelSensor);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "storage")) {
            String systemStorage = MetaJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_storage", app.getResolver()));
            if (systemStorage.length() > 0) {
                jsonObj.put("storage", systemStorage);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "memory")) {
            String systemMemory = MetaJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_memory", app.getResolver()));
            if (systemMemory.length() > 0) {
                jsonObj.put("memory", systemMemory);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "cpu")) {
            String systemCPU = MetaJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_cpu", app.getResolver()));
            if (systemCPU.length() > 0) {
                jsonObj.put("cpu", systemCPU);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "network")) {
            String systemNetwork = MetaJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values", "system_network", app.getResolver()));
            if (systemNetwork.length() > 0) {
                jsonObj.put("network", systemNetwork);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "swm")) {
            String swmDiagnostic = MetaJsonUtils.getConcatMetaField(RfcxComm.getQuery("admin", "get_momentary_values",
                    "swm_diagnostic", app.getResolver()));
            if (swmDiagnostic.length() > 0) {
                jsonObj.put("swm", swmDiagnostic);
            }
        }

        if ((includeAllExtraFields && (includeAssetBundleCount > 0)) || ArrayUtils.doesStringArrayContainString(includeExtraFields, "meta")) {
            jsonObj = app.metaJsonUtils.retrieveAndBundleMetaJson(jsonObj, Math.max(includeAssetBundleCount, 1), false);
            includePurgedAssetList = true;
        }

        if ((includeAllExtraFields && (includeAssetBundleCount > 0)) || ArrayUtils.doesStringArrayContainString(includeExtraFields, "detections")) {
            if (app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).equalsIgnoreCase("sat")) {
                jsonObj = app.audioDetectionJsonUtils.retrieveAndBundleDetectionJson(jsonObj, Math.max(includeAssetBundleCount, 1), false, true);
                includePurgedAssetList = false;
            } else {
                jsonObj = app.audioDetectionJsonUtils.retrieveAndBundleDetectionJson(jsonObj, Math.max(includeAssetBundleCount, 1), false, false);
                includePurgedAssetList = true;
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "checkins")) {
            jsonObj.put("checkins", app.metaJsonUtils.getCheckInStatusInfoForJson(new String[]{}));
            jsonObj.put("measured_at", "" + System.currentTimeMillis());
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "purged") || includePurgedAssetList) {
            jsonObj.put("purged", app.assetUtils.getAssetExchangeLogList("purged", 4 * app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_META_SEND_BUNDLE_LIMIT)));
        }

        if (ArrayUtils.doesStringArrayContainString(includeExtraFields, "companion")) {
            jsonObj.put("companion", app.companionSocketUtils.getCompanionPingJsonObj());
        }

        if (printJsonToLogs) {
            int limitLogsTo = 1800;
            JSONObject jsonLogObj = new JSONObject(jsonObj.toString());
            for (String excludeField : excludeFieldsFromLogs) {
                jsonLogObj.remove(excludeField);
                jsonLogObj.put(excludeField, "{...}");
            }
            String strLogs = jsonLogObj.toString();
            Log.d(logTag, (strLogs.length() <= limitLogsTo) ? strLogs : strLogs.substring(0, limitLogsTo) + "...");
        }

        if (shouldShorten) {
            return ApiPingExt.INSTANCE.shortenPingJson(jsonObj).toString();
        }
        return jsonObj.toString();
    }


}
