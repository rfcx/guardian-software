package guardian.device.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import guardian.RfcxGuardian;
import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.DeviceCPU;
import rfcx.utility.misc.ArrayUtils;
import rfcx.utility.rfcx.RfcxLog;

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

	public static final long[] geolocationMinDistanceChangeBetweenUpdatesInMeters = 	new long[] {		33, 		5 	};
	public static final long[] geolocationMinTimeElapsedBetweenUpdatesInSeconds = 	new long[] { 	300,		20 	};
	
	private List<double[]> accelSensorSnapshotValues = new ArrayList<double[]>();

	private List<double[]> recentValuesAccelSensor = new ArrayList<double[]>();
	private List<double[]> recentValuesGeoLocation = new ArrayList<double[]>();
	
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
	
	public static double[] generateAverageAccelValues(List<double[]> accelValues) {
		
		// initialize array of averages
		double[] avgs = new double[5]; Arrays.fill(avgs, 0);
		
		if (accelValues.size() > 0) {
			avgs = ArrayUtils.limitArrayValuesToSpecificDecimalPlaces( ArrayUtils.getAverageValuesAsArrayFromArrayList(accelValues), 6 );
			avgs[4] = accelValues.size();	// number of samples in average
			// find the most recent sampled timestamp
			for (double[] accelVals : accelValues) { if (accelVals[0] > avgs[0]) { avgs[0] = (long) Math.round(accelVals[0]); } }
		}
		return avgs;
	}

	public void addAccelSensorSnapshotEntry(double[] accelSensorSnapshotEntry) {
		this.accelSensorSnapshotValues.add(accelSensorSnapshotEntry);
	}
	
	public void processAccelSensorSnapshot() {
		
		double[] accelSensorSnapshotAverages = generateAverageAccelValues(this.accelSensorSnapshotValues);
		this.accelSensorSnapshotValues = new ArrayList<double[]>();
		
		if ((accelSensorSnapshotAverages.length == 5) && (accelSensorSnapshotAverages[4] > 0)) {
			
			if (app.rfcxPrefs.getPrefAsBoolean("verbose_logging")) { 
				Log.i(logTag, "Snapshot —— Accelerometer"
						+" —— x: "+accelSensorSnapshotAverages[1]+", y: "+accelSensorSnapshotAverages[2]+", z: "+accelSensorSnapshotAverages[3]
						+" —— "+Math.round(accelSensorSnapshotAverages[4])+" sample(s)"
						+" —— "+DateTimeUtils.getDateTime((long) Math.round(accelSensorSnapshotAverages[0]))
						);
			}
			
			// this is where we would report this interim accel value to something, somewhere that would determine if the phone is moving around...			
		}
	}
	
	public double[] getParsedGeoLocation(Location location) {
		if (location != null) {
			try {
				
				double[] geoLoc = new double[] { 
						(double) System.currentTimeMillis(),
						location.getLatitude(), location.getLongitude(), 
						(double) location.getAccuracy(), 
						location.getAltitude(),
						(double) location.getTime()
					};
				
				if (app.rfcxPrefs.getPrefAsBoolean("verbose_logging")) { 
					Log.i(logTag, "Snapshot —— GeoLocation"
							+" —— Lat: "+geoLoc[1]+", Lng: "+geoLoc[2]+", Alt: "+Math.round(geoLoc[4])+" meters"
							+" —— Accuracy: "+Math.round(geoLoc[3])+" meters"
							+" —— "+DateTimeUtils.getDateTime((long) Math.round(geoLoc[0]))
							);
				}
				
				return geoLoc;
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return new double[] {};
	}

}
