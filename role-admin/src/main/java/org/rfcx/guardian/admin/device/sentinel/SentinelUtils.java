package org.rfcx.guardian.admin.device.sentinel;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.DeviceI2cUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentinelUtils {

    public static final long captureLoopIncrementFullDurationInMilliseconds = 1200;
    public static final long captureCycleMinimumAllowedDurationInMilliseconds = 20000;
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


}
