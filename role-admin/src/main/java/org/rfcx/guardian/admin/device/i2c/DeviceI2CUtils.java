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

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DeviceI2CUtils");

    public static final int inReducedCaptureModeExtendCaptureCycleByFactorOf = 3;

    public static void setSentinelLoggingVerbosity(Context context) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        boolean isVerbose = app.rfcxPrefs.getPrefAsBoolean( RfcxPrefs.Pref.ADMIN_VERBOSE_SENTINEL );
        app.sentinelPowerUtils.verboseLogging = isVerbose;
        app.sentryAccelUtils.verboseLogging = isVerbose;
    }

    public static JSONArray getI2cSensorValuesAsJsonArray(Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray sensorJsonArray = new JSONArray();
        try {
            JSONObject sensorJson = new JSONObject();
            sensorJson.put("environment", app.sentinelSensorDb.dbEnvironment.getConcatRowsWithLabelPrepended("environment"));
            sensorJson.put("battery", app.sentinelSensorDb.dbBattery.getConcatRowsWithLabelPrepended("battery"));
            sensorJsonArray.put(sensorJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        } finally {
            return sensorJsonArray;
        }
    }

    public static int deleteI2cSensorValuesBeforeTimestamp(String timeStamp, Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

        Date clearBefore = new Date(Long.parseLong(timeStamp));

        app.sentinelSensorDb.dbEnvironment.clearRowsBefore(clearBefore);
        app.sentinelSensorDb.dbBattery.clearRowsBefore(clearBefore);

        return 1;
    }


    public static JSONArray getMomentaryI2cSensorValuesAsJsonArray(boolean forceUpdate, Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray sensorJsonArray = new JSONArray();

        if (forceUpdate) {

            if (app.sentryAccelUtils.isCaptureAllowed()) {
                app.sentryAccelUtils.resetAccelValues();
                app.sentryAccelUtils.updateSentryAccelValues();
            }

//            if (app.sentinelCompassUtils.isCaptureAllowed()) {
//                app.sentinelCompassUtils.resetCompassValues();
//                app.sentinelCompassUtils.updateSentinelCompassValues();
//            }
        }

        try {
            JSONObject sensorJson = new JSONObject();

            if (app.sentryAccelUtils.getAccelValues().size() > 0) {
                long[] accelVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(app.sentryAccelUtils.getAccelValues()));
                sensorJson.put("accelerometer", "accelerometer*"+accelVals[4]+"*"+accelVals[0]+"*"+accelVals[1]+"*"+accelVals[2]+"*"+accelVals[3] );
            }

//            if (app.sentinelCompassUtils.getCompassValues().size() > 0) {
//                long[] compassVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(app.sentinelCompassUtils.getCompassValues()));
//                sensorJson.put("compass", "compass*"+compassVals[4]+"*"+compassVals[0]+"*"+compassVals[1]+"*"+compassVals[2]+"*"+compassVals[3]);
//            }

            sensorJsonArray.put(sensorJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        }

        return sensorJsonArray;
    }

}
