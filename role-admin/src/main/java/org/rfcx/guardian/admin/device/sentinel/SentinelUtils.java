package org.rfcx.guardian.admin.device.sentinel;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.device.android.system.DeviceUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.Date;

public class SentinelUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentinelUtils");

    public static final int inReducedCaptureModeExtendCaptureCycleByFactorOf = 3;

    public static void setVerboseSentinelLogging(Context context) {
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        boolean isVerbose = app.rfcxPrefs.getPrefAsBoolean( RfcxPrefs.Pref.ADMIN_VERBOSE_SENTINEL );
        app.sentinelPowerUtils.verboseLogging = isVerbose;
        app.sentinelAccelUtils.verboseLogging = isVerbose;
        app.sentinelCompassUtils.verboseLogging = isVerbose;
    }

    public static JSONArray getSentinelSensorValuesAsJsonArray(Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray accelJsonArray = new JSONArray();
        try {
            JSONObject sensorJson = new JSONObject();
            sensorJson.put("accelerometer", app.sentinelSensorDb.dbAccelerometer.getConcatRowsWithLabelPrepended("accelerometer"));
            sensorJson.put("compass", app.sentinelSensorDb.dbCompass.getConcatRowsWithLabelPrepended("compass"));
            accelJsonArray.put(sensorJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        } finally {
            return accelJsonArray;
        }
    }

    public static int deleteSentinelSensorValuesBeforeTimestamp(String timeStamp, Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

        Date clearBefore = new Date(Long.parseLong(timeStamp));

        app.sentinelSensorDb.dbAccelerometer.clearRowsBefore(clearBefore);
        app.sentinelSensorDb.dbCompass.clearRowsBefore(clearBefore);

        return 1;
    }


    public static JSONArray getMomentarySentinelSensorValuesAsJsonArray(boolean forceUpdate, Context context) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
        JSONArray sensorJsonArray = new JSONArray();

        if (forceUpdate) {

            if (app.sentinelAccelUtils.isCaptureAllowed()) {
                app.sentinelAccelUtils.resetAccelValues();
                app.sentinelAccelUtils.updateSentinelAccelValues();
            }

            if (app.sentinelCompassUtils.isCaptureAllowed()) {
                app.sentinelCompassUtils.resetCompassValues();
                app.sentinelCompassUtils.updateSentinelCompassValues();
            }
        }

        try {
            JSONObject sensorJson = new JSONObject();

            if (app.sentinelAccelUtils.getAccelValues().size() > 0) {
                long[] accelVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(app.sentinelAccelUtils.getAccelValues()));
                sensorJson.put("accelerometer", "accelerometer*"+accelVals[4]+"*"+accelVals[0]+"*"+accelVals[1]+"*"+accelVals[2]+"*"+accelVals[3] );
            }

            if (app.sentinelCompassUtils.getCompassValues().size() > 0) {
                long[] compassVals = ArrayUtils.roundArrayValuesAndCastToLong(ArrayUtils.getAverageValuesAsArrayFromArrayList(app.sentinelCompassUtils.getCompassValues()));
                sensorJson.put("compass", "compass*"+compassVals[4]+"*"+compassVals[0]+"*"+compassVals[1]+"*"+compassVals[2]+"*"+compassVals[3]);
            }

            sensorJsonArray.put(sensorJson);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        } finally {
            return sensorJsonArray;
        }
    }

}
