package org.rfcx.guardian.admin.companion;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.comms.sms.SmsUtils;
import org.rfcx.guardian.admin.device.i2c.DeviceI2CUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.Iterator;

public class CompanionPingJsonUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CompanionPingJsonUtils");
    private RfcxGuardian app;

    public CompanionPingJsonUtils(Context context) {

        this.app = (RfcxGuardian) context.getApplicationContext();

    }

    private static String getConcatMetaField(JSONArray metaJsonArray) throws JSONException {
        ArrayList<String> metaBlobs = new ArrayList<String>();
        for (int i = 0; i < metaJsonArray.length(); i++) {
            JSONObject metaJsonRow = metaJsonArray.getJSONObject(i);
            Iterator<String> paramLabels = metaJsonRow.keys();
            while (paramLabels.hasNext()) {
                String paramLabel = paramLabels.next();
                if ((metaJsonRow.get(paramLabel) instanceof String) && (metaJsonRow.getString(paramLabel).length() > 0)) {
                    metaBlobs.add(metaJsonRow.getString(paramLabel));
                }
            }
        }
        return (metaBlobs.size() > 0) ? TextUtils.join("|", metaBlobs) : "";
    }

    public String buildPingJson(boolean includeAllExtraFields, String[] includeExtraFields, int includeAssetBundleCount, boolean printJsonToLogs, String[] excludeFieldsFromLogs) throws JSONException {

        JSONObject jsonObj = new JSONObject();

        boolean includeMeasuredAt = false;

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sms")) {
            JSONArray smsArr = SmsUtils.getSmsMessagesAsJsonArray(app);
            if (smsArr.length() > 0) {
                jsonObj.put("messages", smsArr);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_power")) {
            String sentinelPower = getConcatMetaField(app.sentinelPowerUtils.getMomentarySentinelPowerValuesAsJsonArray());
            if (sentinelPower.length() > 0) {
                jsonObj.put("sentinel_power", sentinelPower);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "sentinel_sensor")) {
            String sentinelSensor = getConcatMetaField(DeviceI2CUtils.getMomentaryI2cSensorValuesAsJsonArray(true, app.getApplicationContext()));
            if (sentinelSensor.length() > 0) {
                jsonObj.put("sentinel_sensor", sentinelSensor);
            }
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "storage")) {
            String systemStorage = getConcatMetaField(app.deviceUtils.getMomentaryConcatSystemMetaValuesAsJsonArray("storage"));
            if (systemStorage.length() > 0) {
                jsonObj.put("storage", systemStorage);
            }
            includeMeasuredAt = true;
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "memory")) {
            String systemMemory = getConcatMetaField(app.deviceUtils.getMomentaryConcatSystemMetaValuesAsJsonArray("memory"));
            if (systemMemory.length() > 0) {
                jsonObj.put("memory", systemMemory);
            }
            includeMeasuredAt = true;
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "cpu")) {
            String systemCPU = getConcatMetaField(app.deviceUtils.getMomentaryConcatSystemMetaValuesAsJsonArray("cpu"));
            if (systemCPU.length() > 0) {
                jsonObj.put("cpu", systemCPU);
            }
            includeMeasuredAt = true;
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "network")) {
            String systemNetwork = getConcatMetaField(app.deviceUtils.getMomentaryConcatSystemMetaValuesAsJsonArray("network"));
            if (systemNetwork.length() > 0) {
                jsonObj.put("network", systemNetwork);
            }
            includeMeasuredAt = true;
        }

        if (includeMeasuredAt) {
            jsonObj.put("measured_at", System.currentTimeMillis());
        }

        if (includeAllExtraFields || ArrayUtils.doesStringArrayContainString(includeExtraFields, "companion")) {
            JSONObject companionJsonObj = new JSONObject();
            jsonObj.put("companion", companionJsonObj);
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

        return jsonObj.toString();
    }

}
