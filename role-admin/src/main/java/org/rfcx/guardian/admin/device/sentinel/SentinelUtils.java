package org.rfcx.guardian.admin.device.sentinel;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.Date;

public class SentinelUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SentinelUtils");

    public static final long captureLoopIncrementFullDurationInMilliseconds = 1000;
    public static final long captureCycleMinimumAllowedDurationInMilliseconds = 30000;
    public static final double captureCycleDurationRatioComparedToAudioCycleDuration = 0.66666667;

    public static final int inReducedCaptureModeExtendCaptureCycleByFactorOf = 2;

    public static long getCaptureCycleDuration(int audioCycleDurationInSeconds) {
        long captureCycleDuration = Math.round( audioCycleDurationInSeconds * 1000 * captureCycleDurationRatioComparedToAudioCycleDuration );
        if (captureCycleDuration < captureCycleMinimumAllowedDurationInMilliseconds) {
            captureCycleDuration = captureCycleMinimumAllowedDurationInMilliseconds;
        }
        return captureCycleDuration;
    }

    public static int getInnerLoopsPerCaptureCycle(int audioCycleDurationInSeconds) {
        return Math.round( getCaptureCycleDuration(audioCycleDurationInSeconds) / captureLoopIncrementFullDurationInMilliseconds );
    }

    public static int getOuterLoopCaptureCount(int audioCycleDurationInSeconds) {
//		return (int) ( Math.round( geoPositionMinTimeElapsedBetweenUpdatesInSeconds[0] / ( getCaptureCycleDuration(audioCycleDurationInSeconds) / 1000 ) ) );
        return 2;
    }

    public static long getInnerLoopDelayRemainder(int audioCycleDurationInSeconds, double captureCycleDurationPercentageMultiplier, long samplingOperationDuration) {
        return (long) ( Math.round( ( getCaptureCycleDuration(audioCycleDurationInSeconds) / getInnerLoopsPerCaptureCycle(audioCycleDurationInSeconds) ) - samplingOperationDuration ) * captureCycleDurationPercentageMultiplier );
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

}
