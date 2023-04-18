package org.rfcx.guardian.admin.device.i2c;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.Date;

public class DeviceI2CUtils {

    public static final int inReducedCaptureModeExtendCaptureCycleByFactorOf = 3;
    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DeviceI2CUtils");

    public static void setSentinelLoggingVerbosity(Context context) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        boolean isVerbose = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_VERBOSE_SENTINEL);
        app.sentinelPowerUtils.verboseLogging = isVerbose;
    }

    public static JSONArray getI2cSensorValuesAsJsonArray(Context context) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray sensorJsonArray = new JSONArray();
        try {

            String bmeValues = app.sentrySensorDb.dbBME688.getConcatRowsIgnoreNull("bme688");
            if (bmeValues != null) {
                JSONObject bmeObj = new JSONObject();
                bmeObj.put("bme688", bmeValues);
                sensorJsonArray.put(bmeObj);
            }

            String infValues = app.sentrySensorDb.dbInfineon.getConcatRowsIgnoreNull("infineon");
            if (infValues != null) {
                JSONObject infObj = new JSONObject();
                infObj.put("infineon", infValues);
                sensorJsonArray.put(infObj);
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return sensorJsonArray;
    }

    public static int deleteI2cSensorValuesBeforeTimestamp(String timeStamp, Context context) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

        Date clearBefore = new Date(Long.parseLong(timeStamp));
        app.sentrySensorDb.dbBME688.clearRowsBefore(clearBefore);
        app.sentrySensorDb.dbInfineon.clearRowsBefore(clearBefore);

        return 1;
    }


    public static JSONArray getMomentaryI2cSensorValuesAsJsonArray(boolean forceUpdate, Context context) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray sensorJsonArray = new JSONArray();

        if (forceUpdate) {
            if (app.sentryBME688Utils.isChipAccessibleByI2c()) {
                app.sentryBME688Utils.resetBMEValues();
                app.sentryBME688Utils.saveBME688ValuesToDatabase(app.sentryBME688Utils.getBME688Values());
            }

            if (app.sentryInfineonUtils.isChipAccessibleByI2c()) {
                app.sentryInfineonUtils.resetInfineonValues();
                app.sentryInfineonUtils.saveInfineonValuesToDatabase(app.sentryInfineonUtils.getInfineonValues());
            }
        }

        try {
            JSONObject sensorJson = new JSONObject();

            if (app.sentryBME688Utils.getCurrentBMEValues() != null) {
                sensorJson.put("bme688", app.sentrySensorDb.dbBME688.getConcatRowsIgnoreNull("bme688"));
            }

            if (app.sentryInfineonUtils.getCurrentInfineonValues() != null) {
                sensorJson.put("infineon", app.sentrySensorDb.dbInfineon.getConcatRowsIgnoreNull("infineon"));
            }

            sensorJsonArray.put(sensorJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        }

        return sensorJsonArray;
    }

}
