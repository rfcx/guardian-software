package org.rfcx.guardian.admin.device.android.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import android.Manifest;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.device.android.system.DeviceSystemService.SignalStrengthListener;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceCPU;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DeviceUtils {

	public DeviceUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceUtils.class);

	private RfcxGuardian app = null;
	
	public SignalStrengthListener signalStrengthListener;
	public TelephonyManager telephonyManager;
	public SignalStrength telephonySignalStrength;
	public LocationManager locationManager;
	public SensorManager sensorManager;
	
	public Sensor lightSensor;
	public Sensor accelSensor;

	public String geoPositionProviderInfo;
	
	public double lightSensorLastValue = Float.MAX_VALUE;
	
//	public boolean isListenerRegistered_telephony = false;
//	public boolean isListenerRegistered_light = false;
//	public boolean isListenerRegistered_accel = false;
//	public boolean isListenerRegistered_geoposition = false;

//	public boolean allowListenerRegistration_telephony = true;
//	public boolean allowListenerRegistration_light = true;
//	public boolean allowListenerRegistration_accel = true;
//	public boolean allowListenerRegistration_geolocation = true;
//	public boolean allowListenerRegistration_geolocation_gps = true;
//	public boolean allowListenerRegistration_geolocation_network = true;
	
	public long dateTimeDiscrepancyFromSystemClock_gps = 0;
	public long dateTimeDiscrepancyFromSystemClock_sntp = 0;
	public long dateTimeSourceLastSyncedAt_gps = 0;
	public long dateTimeSourceLastSyncedAt_sntp = 0;
	
	public static final long captureLoopIncrementFullDurationInMilliseconds = 1000;
	public static final long captureCycleMinimumAllowedDurationInMilliseconds = 20000;
	public static final double captureCycleDurationRatioComparedToAudioCycleDuration = 0.66666667;
	
	public static final int accelSensorSnapshotsPerCaptureCycle = 2;

	public static final long[] geoPositionMinDistanceChangeBetweenUpdatesInMeters = 	new long[] {		1, 		1,		1 	};
	public static final long[] geoPositionMinTimeElapsedBetweenUpdatesInSeconds = 	new long[] { 	180/*3600*/,	60,		10 	};
	public int geoPositionUpdateIndex = 0;
	
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
	
	public static int getInnerLoopsPerCaptureCycle(int audioCycleDurationInSeconds) {
		return Math.round( getCaptureCycleDuration(audioCycleDurationInSeconds) / captureLoopIncrementFullDurationInMilliseconds );
	}
	
	public static long getInnerLoopDelayRemainder(int audioCycleDurationInSeconds) {
		return (long) ( Math.round( getCaptureCycleDuration(audioCycleDurationInSeconds) / getInnerLoopsPerCaptureCycle(audioCycleDurationInSeconds) ) - DeviceCPU.SAMPLE_DURATION_MILLISECONDS );
	}
	
	public static int getOuterLoopCaptureCount(int audioCycleDurationInSeconds) {
		return (int) ( Math.round( geoPositionMinTimeElapsedBetweenUpdatesInSeconds[0] / ( getCaptureCycleDuration(audioCycleDurationInSeconds) / 1000 ) ) );
	}
	
	public static double[] generateAverageAccelValues(List<double[]> accelValues) {
		
		// initialize array of averages
		double[] avgs = new double[5]; Arrays.fill(avgs, 0);
		
		if (accelValues.size() > 0) {
			avgs = ArrayUtils.limitArrayValuesToSpecificDecimalPlaces( ArrayUtils.getAverageValuesAsArrayFromArrayList(accelValues), 6 );
			avgs[4] = accelValues.size();	// number of samples in average
			// find the most recent sampled timestamp
			avgs[0] = 0; for (double[] accelVals : accelValues) { if (accelVals[0] > avgs[0]) { avgs[0] = (long) Math.round(accelVals[0]); } }
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

	public static boolean isAppRoleApprovedForGeoPositionAccess(Context context) {

		return (	(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
				&&	(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			//	&&	(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
				);
	}
	
	public void processAndSaveGeoPosition(Location location) {
		if (location != null) {
			try {
				
				dateTimeSourceLastSyncedAt_gps = System.currentTimeMillis();
				dateTimeDiscrepancyFromSystemClock_gps = DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(location.getTime());
				
				double[] geoPos = new double[] { 
						(double) dateTimeSourceLastSyncedAt_gps,
						location.getLatitude(), location.getLongitude(), 
						(double) location.getAccuracy(), 
						location.getAltitude(),
						(double) location.getTime()
					};
				
				if (app.rfcxPrefs.getPrefAsBoolean("verbose_logging")) { 
					Log.i(logTag, "Snapshot —— GeoPosition"
							+" —— Lat: "+geoPos[1]+", Lng: "+geoPos[2]+", Alt: "+Math.round(geoPos[4])+" meters"
							+" —— Accuracy: "+Math.round(geoPos[3])+" meters"
							+" —— "+DateTimeUtils.getDateTime(dateTimeSourceLastSyncedAt_gps)
							+" —— Clock Discrepancy: "+dateTimeDiscrepancyFromSystemClock_gps+" ms"
							);
				}
				
				// only save/cache geoposition values if the GPS clock is less than 5 minutes different than the system clock
				if (Math.abs(dateTimeDiscrepancyFromSystemClock_gps) < (5 * 60 * 1000) ) {
					
					app.deviceSystemDb.dbDateTimeOffsets.insert(dateTimeSourceLastSyncedAt_gps, "gps", dateTimeDiscrepancyFromSystemClock_gps);
					app.deviceSensorDb.dbGeoPosition.insert(geoPos[0], geoPos[1], geoPos[2], geoPos[3], geoPos[4]);
					
				} else {
					Log.e(logTag, "Not saving GeoPosition to database...");
				}
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}
	}

	public static JSONArray getSystemMetaValuesAsJsonArray(Context context) {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		JSONArray metaJsonArray = new JSONArray();
		try {
			JSONObject metaJson = new JSONObject();

			metaJson.put("battery", app.deviceSystemDb.dbBattery.getConcatRows());
			metaJson.put("cpu", app.deviceSystemDb.dbCPU.getConcatRows());
			metaJson.put("power", app.deviceSystemDb.dbPower.getConcatRows());
			metaJson.put("network", app.deviceSystemDb.dbTelephony.getConcatRows());
			metaJson.put("offline", app.deviceSystemDb.dbOffline.getConcatRows());
			metaJson.put("lightmeter", app.deviceSensorDb.dbLightMeter.getConcatRows());
			metaJson.put("accelerometer", app.deviceSensorDb.dbAccelerometer.getConcatRows());
			metaJson.put("data_transfer", app.deviceDataTransferDb.dbTransferred.getConcatRows());
			metaJson.put("reboots", app.rebootDb.dbRebootComplete.getConcatRows());
			metaJson.put("geoposition", app.deviceSensorDb.dbGeoPosition.getConcatRows());
			metaJson.put("disk_usage", app.deviceDiskDb.dbDiskUsage.getConcatRows());
			metaJsonArray.put(metaJson);

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);

		} finally {
			return metaJsonArray;
		}
	}

	public static int deleteSystemMetaValuesBeforeTimestamp(String timeStamp, Context context) {

		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

		Date clearBefore = new Date(Long.parseLong(timeStamp));

		app.deviceSystemDb.dbBattery.clearRowsBefore(clearBefore);
		app.deviceSystemDb.dbCPU.clearRowsBefore(clearBefore);
		app.deviceSystemDb.dbPower.clearRowsBefore(clearBefore);
		app.deviceSystemDb.dbTelephony.clearRowsBefore(clearBefore);
		app.deviceSystemDb.dbOffline.clearRowsBefore(clearBefore);
		app.deviceSensorDb.dbLightMeter.clearRowsBefore(clearBefore);
		app.deviceSensorDb.dbAccelerometer.clearRowsBefore(clearBefore);
		app.deviceDataTransferDb.dbTransferred.clearRowsBefore(clearBefore);
		app.rebootDb.dbRebootComplete.clearRowsBefore(clearBefore);
		app.deviceSensorDb.dbGeoPosition.clearRowsBefore(clearBefore);
		app.deviceDiskDb.dbDiskUsage.clearRowsBefore(clearBefore);

		return 1;
	}

}
