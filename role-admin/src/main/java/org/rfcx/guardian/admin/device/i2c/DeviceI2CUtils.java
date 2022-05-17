package org.rfcx.guardian.admin.device.i2c;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.device.i2c.sentry.bme.BME688Att;
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
        app.sentryAccelUtils.verboseLogging = isVerbose;
    }

    public static JSONArray getI2cSensorValuesAsJsonArray(Context context) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray sensorJsonArray = new JSONArray();
        try {
            JSONObject sensorJson = new JSONObject();

            String bmeValues = app.sentrySensorDb.dbBME688.getConcatRowsWithLabelPrepended("bme688");
            if (!bmeValues.split("\\*")[2].equalsIgnoreCase("0")) {
                sensorJson.put("sentinel_sensor", app.sentrySensorDb.dbBME688.getConcatRowsWithLabelPrepended("bme688"));
            }
            sensorJsonArray.put(sensorJson);
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return sensorJsonArray;
    }

    public static int deleteI2cSensorValuesBeforeTimestamp(String timeStamp, Context context) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

        Date clearBefore = new Date(Long.parseLong(timeStamp));
        app.sentrySensorDb.dbBME688.clearRowsBefore(clearBefore);

        return 1;
    }


    public static JSONArray getMomentaryI2cSensorValuesAsJsonArray(boolean forceUpdate, Context context) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray sensorJsonArray = new JSONArray();

        if (forceUpdate) {
            if (app.sentryAccelUtils.isChipAccessibleByI2c()) {
                app.sentryAccelUtils.resetAccelValues();
                app.sentryAccelUtils.updateSentryAccelValues();
            }

            if (app.sentryBME688Utils.isChipAccessibleByI2c()) {
                app.sentryBME688Utils.resetBMEValues();
                app.sentryBME688Utils.updateSentryBMEValues();
            }
        }

        try {
            JSONObject sensorJson = new JSONObject();

            if (app.sentryAccelUtils.getAccelValues().size() > 0) {
                long[] accelVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(app.sentryAccelUtils.getAccelValues()));
                sensorJson.put("accelerometer", "accelerometer*" + accelVals[4] + "*" + accelVals[0] + "*" + accelVals[1] + "*" + accelVals[2] + "*" + accelVals[3]);
            }

            if (app.sentryBME688Utils.getCurrentBMEValues() != null) {
                String bmeValues = app.sentryBME688Utils.getCurrentBMEValues().toString();
                sensorJson.put("bme688", bmeValues);
            }

            sensorJsonArray.put(sensorJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        }

        return sensorJsonArray;
    }

}
