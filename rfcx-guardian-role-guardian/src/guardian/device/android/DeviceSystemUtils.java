package guardian.device.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.DeviceCPU;
import rfcx.utility.device.DeviceDiskUsage;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.rfcx.RfcxLog;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.text.TextUtils;
import android.util.Log;
import guardian.RfcxGuardian;

public class DeviceSystemUtils {

	public DeviceSystemUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceSystemUtils.class);

	private RfcxGuardian app = null;
	
	public static final long captureLoopIncrementFullDurationInMilliseconds = 1000;
	public static final long captureCycleMinimumAllowedDurationInMilliseconds = 20000;
	public static final double captureCycleDurationRatioComparedToAudioCycleDuration = 0.66666667;
	
	public static final int accelSensorSnapshotsPerCaptureCycle = 2;
	public static final int accelSensorValueFloatMultiplier = 1000000;

	public static final long gpsMinimumDistanceChangeBetweenUpdatesInMeters = 10;
	public static final long gpsMinimumTimeElapsedBetweenUpdatesInMilliseconds = 15000;
	
	private List<long[]> accelSensorSnapshotValues = new ArrayList<long[]>();

	private List<double[]> recentValuesAccelSensor = new ArrayList<double[]>();
	private List<double[]> recentValuesGeoLocation = new ArrayList<double[]>();
	
	
	// this just inserts dummy values...
	public double[] getCurrentGeoLocation() {
		String[] prefsGeo = app.rfcxPrefs.getPrefAsString("geolocation_override").split(",");
		return new double[] { System.currentTimeMillis(), (double) Double.parseDouble(prefsGeo[0]), (double) Double.parseDouble(prefsGeo[1]), 20 };
	}
	
	public static long getCaptureCycleDuration(int audioCycleDurationInSeconds) {
		long captureCycleDuration = (long) Math.round( audioCycleDurationInSeconds * 1000 * captureCycleDurationRatioComparedToAudioCycleDuration );
		if (captureCycleDuration < captureCycleMinimumAllowedDurationInMilliseconds) { 
			captureCycleDuration = captureCycleMinimumAllowedDurationInMilliseconds;
		}
		return captureCycleDuration;
	}
	
	public static int getLoopsPerCaptureCycle(int audioCycleDurationInSeconds) {
		return Math.round( getCaptureCycleDuration(audioCycleDurationInSeconds) / captureLoopIncrementFullDurationInMilliseconds );
	}
	
	public static long getLoopDelayRemainder(int audioCycleDurationInSeconds) {
		return (long) ( Math.round( getCaptureCycleDuration(audioCycleDurationInSeconds) / getLoopsPerCaptureCycle(audioCycleDurationInSeconds) ) - DeviceCPU.SAMPLE_DURATION_MILLISECONDS );
	}
	
	
	
	
	public static double[] generateAverageAccelValue(List<long[]> accelValues) {

		double[] avgAccelVals = new double[] { 0, 0, 0 };
		int sampleCount = accelValues.size();
		long measuredAt = 0;
		
		if (sampleCount > 0) {
			// sum up all values
			for (long[] accelVals : accelValues) {
				for (int i = 0; i < avgAccelVals.length; i++) {
					avgAccelVals[i] = avgAccelVals[i] + (((double) accelVals[i+1]) / accelSensorValueFloatMultiplier);
				}
				if (accelVals[0] > measuredAt) { measuredAt = accelVals[0]; }
			}
			
			for (int i = 0; i < avgAccelVals.length; i++) {
				avgAccelVals[i] = avgAccelVals[i] / sampleCount;
			}
		}
		
		for (int i = 0; i < avgAccelVals.length; i++) {
			avgAccelVals[i] = ((double) Math.round(avgAccelVals[i] * accelSensorValueFloatMultiplier)) / accelSensorValueFloatMultiplier;
		}
		
		return new double[] { measuredAt, avgAccelVals[0], avgAccelVals[1], avgAccelVals[2], sampleCount };
	}

	public void addAccelSensorSnapshotEntry(long[] accelSensorSnapshotEntry) {
		this.accelSensorSnapshotValues.add(accelSensorSnapshotEntry);
	}
	
	public void processAccelSensorSnapshot() {
		double[] accelSensorSnapshotAverages = generateAverageAccelValue(this.accelSensorSnapshotValues);
		this.accelSensorSnapshotValues = new ArrayList<long[]>();
		
		if (accelSensorSnapshotAverages[4] > 0) {
			
			if (app.rfcxPrefs.getPrefAsBoolean("verbose_logging")) { 
				Log.i(logTag, "Snapshot —— Accelerometer"
						+" —— x: "+accelSensorSnapshotAverages[1]+", y: "+accelSensorSnapshotAverages[2]+", z: "+accelSensorSnapshotAverages[3]
						+" —— "+Math.round(accelSensorSnapshotAverages[4])+" sample(s)"
						+" —— "+DateTimeUtils.getDateTime((long) Math.round(accelSensorSnapshotAverages[0]))
						);
			}
			// this is where we would report this interim accel value to some something, somewhere that would determine if the phone is moving around...			
		}
	}

}
