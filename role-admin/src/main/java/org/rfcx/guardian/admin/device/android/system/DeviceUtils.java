package org.rfcx.guardian.admin.device.android.system;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.device.capture.DeviceMemory;
import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DeviceUtils {

	public DeviceUtils(Context context) {
		this.context = context;
		this.app = (RfcxGuardian) context;
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DeviceUtils");

	private Context context;
	private RfcxGuardian app;

	public static void setSystemLoggingVerbosity(Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		app.deviceCPU.verboseLogging = app.rfcxPrefs.getPrefAsBoolean( RfcxPrefs.Pref.ADMIN_VERBOSE_CPU );
	}

	private boolean allowListenerRegistration_telephony = true;
	private boolean allowListenerRegistration_light = true;
	private boolean allowListenerRegistration_accel = true;
	private boolean allowListenerRegistration_geoposition = true;
	private boolean allowListenerRegistration_geoposition_gps = true;
	private boolean allowListenerRegistration_geoposition_network = true;

	public boolean isSensorListenerAllowed(String sensorAbbrev) {
		if (sensorAbbrev.equalsIgnoreCase("accel")) {
			return this.allowListenerRegistration_accel;
		} else if (sensorAbbrev.equalsIgnoreCase("light")) {
			return this.allowListenerRegistration_light;
		} else if (sensorAbbrev.equalsIgnoreCase("telephony")) {
			return this.allowListenerRegistration_telephony;
		} else if (sensorAbbrev.equalsIgnoreCase("geoposition")) {
			return this.allowListenerRegistration_geoposition && app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_GEOPOSITION_CAPTURE);
		} else if (sensorAbbrev.equalsIgnoreCase("geoposition_gps")) {
			return this.allowListenerRegistration_geoposition_gps;
		} else if (sensorAbbrev.equalsIgnoreCase("geoposition_network")) {
			return this.allowListenerRegistration_geoposition_network;
		} else {
			return false;
		}
	}

	public void allowOrDisableSensorListener(String sensorAbbrev, boolean allowOrDisable) {
		if (sensorAbbrev.equalsIgnoreCase("accel")) {
			this.allowListenerRegistration_accel = allowOrDisable;
		} else if (sensorAbbrev.equalsIgnoreCase("light")) {
			this.allowListenerRegistration_light = allowOrDisable;
		} else if (sensorAbbrev.equalsIgnoreCase("telephony")) {
			this.allowListenerRegistration_telephony = allowOrDisable;
		} else if (sensorAbbrev.equalsIgnoreCase("geoposition")) {
			this.allowListenerRegistration_geoposition = allowOrDisable;
		} else if (sensorAbbrev.equalsIgnoreCase("geoposition_gps")) {
			this.allowListenerRegistration_geoposition_gps = allowOrDisable;
		} else if (sensorAbbrev.equalsIgnoreCase("geoposition_network")) {
			this.allowListenerRegistration_geoposition_network = allowOrDisable;
		}
	}

	public void allowSensorListener(String sensorAbbrev) {
		allowOrDisableSensorListener(sensorAbbrev, true);
	}

	public void disableSensorListener(String sensorAbbrev) {
		allowOrDisableSensorListener(sensorAbbrev, false);
	}

	public boolean allowMeasurement_battery_percentage = true;
	public boolean allowMeasurement_battery_temperature = true;
	public boolean allowMeasurement_battery_is_charging = true;
	public boolean allowMeasurement_battery_is_fully_charged = true;

	//
	// Static constant values for adjusting and tuning the system service behavior
	//

//	public static final long captureLoopIncrementFullDurationInMilliseconds = 1500;
	public static final long captureCycleMinimumAllowedDurationInMilliseconds = 30000;
	public static final double captureCycleDurationRatioComparedToAudioCycleDuration = 0.66666667;

	public static final int inReducedCaptureModeExtendCaptureCycleByFactorOf = 3;

	public static final long geoPositionMinDistanceChangeBetweenUpdatesInMeters = 1;

	private List<double[]> accelSensorSnapshotValues = new ArrayList<double[]>();
	public static final int accelSensorSnapshotsPerCaptureCycle = 2;

	private final List<double[]> recentValuesAccelSensor = new ArrayList<>();
	private final List<double[]> recentValuesGeoLocation = new ArrayList<>();



	public boolean isReducedCaptureModeActive = false;
	public long reducedCaptureModeLastChangedAt = 0;

	public void setOrUnSetReducedCaptureMode() {

		boolean newIsReducedCaptureModeActive =
			(	!app.rfcxStatus.getLocalStatus( RfcxStatus.Group.AUDIO_CAPTURE, RfcxStatus.Type.ALLOWED, false)
			||	!app.rfcxStatus.getFetchedStatus( RfcxStatus.Group.AUDIO_CAPTURE, RfcxStatus.Type.ALLOWED)
			);

		if (this.isReducedCaptureModeActive != newIsReducedCaptureModeActive) {
			this.reducedCaptureModeLastChangedAt = System.currentTimeMillis();
		}

		this.isReducedCaptureModeActive = newIsReducedCaptureModeActive;
	}


	public boolean isReducedCaptureModeChanging(int audioCycleDurationInSeconds) {
		return (reducedCaptureModeLastChangedAt != 0) && (Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(reducedCaptureModeLastChangedAt)) < getCaptureCycleDuration(audioCycleDurationInSeconds));
	}

	public static long getCaptureCycleDuration(int audioCycleDurationInSeconds) {
		return Math.max( Math.round( audioCycleDurationInSeconds * 1000 * captureCycleDurationRatioComparedToAudioCycleDuration ), captureCycleMinimumAllowedDurationInMilliseconds );
	}
	
	public static int getInnerLoopsPerCaptureCycle(int audioCycleDurationInSeconds, int telemetryCaptureCycleInMilliseconds) {
		return Math.round( getCaptureCycleDuration(audioCycleDurationInSeconds) / telemetryCaptureCycleInMilliseconds );
	}
	
	public static long getInnerLoopDelayRemainder(int audioCycleDurationInSeconds, double captureCycleDurationPercentageMultiplier, long samplingOperationDuration, int telemetryCaptureCycleInMilliseconds) {
		return (long) ( Math.round( ( getCaptureCycleDuration(audioCycleDurationInSeconds) / getInnerLoopsPerCaptureCycle(audioCycleDurationInSeconds,telemetryCaptureCycleInMilliseconds) ) - samplingOperationDuration ) * captureCycleDurationPercentageMultiplier );
	}
	
	public static int getOuterLoopCaptureCount(int audioCycleDurationInSeconds) {
//		return (int) ( Math.round( geoPositionMinTimeElapsedBetweenUpdatesInSeconds[0] / ( getCaptureCycleDuration(audioCycleDurationInSeconds) / 1000 ) ) );
		return 3;
	}

	public static int getInnerLoopUponWhichToTriggerStatusCacheUpdate(int audioCycleDurationInSeconds, int innerLoopsPerCaptureCycle) {
		double fullCycleDuration = getCaptureCycleDuration(audioCycleDurationInSeconds);
		return (int) Math.ceil(innerLoopsPerCaptureCycle * ( ( fullCycleDuration - ( 0.9 * RfcxStatus.localCacheExpirationBounds[0] ) ) / fullCycleDuration ));
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

			Log.i(logTag, "Snapshot —— Accelerometer"
					+" —— x: "+accelSensorSnapshotAverages[1]+", y: "+accelSensorSnapshotAverages[2]+", z: "+accelSensorSnapshotAverages[3]
					+" —— "+Math.round(accelSensorSnapshotAverages[4])+" sample(s)"
					+" —— "+DateTimeUtils.getDateTime((long) Math.round(accelSensorSnapshotAverages[0]))
					);

			// this is where we would report this interim accel value to something, somewhere that would determine if the phone is moving around...			
		}
	}

	public static boolean isAppRoleApprovedForGeoPositionAccess(Context context) {
		if (	(context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
			&&	(context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
		//	&&	(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
		) {
			return true;
		} else {
			return false;
		}
	}
	
	public void processAndSaveGeoPosition(Location location) {
		if (location != null) {
			try {

				long dateTimeSourceLastSyncedAt_gps = System.currentTimeMillis();
				long dateTimeDiscrepancyFromSystemClock_gps = DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(location.getTime());
				
				double[] geoPos = new double[] { 
						(double) dateTimeSourceLastSyncedAt_gps,
						location.getLatitude(), location.getLongitude(), 
						(double) location.getAccuracy(), 
						location.getAltitude()
					};

				Log.i(logTag, "Snapshot —— GeoPosition"
						+" —— Lat: "+geoPos[1]+", Lng: "+geoPos[2]+", Alt: "+Math.round(geoPos[4])+" meters"
						+" —— Accuracy: "+Math.round(geoPos[3])+" meters"
						+" —— "+DateTimeUtils.getDateTime(dateTimeSourceLastSyncedAt_gps)
						+" —— Clock Discrepancy: "+dateTimeDiscrepancyFromSystemClock_gps+" ms"
						);

				app.deviceSystemDb.dbDateTimeOffsets.insert(dateTimeSourceLastSyncedAt_gps, "gps", dateTimeDiscrepancyFromSystemClock_gps, DateTimeUtils.getTimeZoneOffset());
				app.deviceSensorDb.dbGeoPosition.insert(geoPos[0], geoPos[1], geoPos[2], geoPos[3], geoPos[4]);
					
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
			metaJson.put("network", app.deviceSystemDb.dbTelephony.getConcatRows());
			metaJson.put("lightmeter", app.deviceSensorDb.dbLightMeter.getConcatRows());
			metaJson.put("accelerometer", app.deviceSensorDb.dbAccelerometer.getConcatRows());
			metaJson.put("data_transfer", app.deviceDataTransferDb.dbTransferred.getConcatRows());
			metaJson.put("reboots", app.rebootDb.dbRebootComplete.getConcatRows());
			metaJson.put("geoposition", app.deviceSensorDb.dbGeoPosition.getConcatRows());
			metaJson.put("storage", app.deviceSpaceDb.dbStorage.getConcatRows());
			metaJson.put("memory", app.deviceSpaceDb.dbMemory.getConcatRows());
			metaJson.put("datetime_offsets", app.deviceSystemDb.dbDateTimeOffsets.getConcatRows());

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
		app.deviceSystemDb.dbTelephony.clearRowsBefore(clearBefore);
		app.deviceSensorDb.dbLightMeter.clearRowsBefore(clearBefore);
		app.deviceSensorDb.dbAccelerometer.clearRowsBefore(clearBefore);
		app.deviceDataTransferDb.dbTransferred.clearRowsBefore(clearBefore);
		app.rebootDb.dbRebootComplete.clearRowsBefore(clearBefore);
		app.deviceSensorDb.dbGeoPosition.clearRowsBefore(clearBefore);
		app.deviceSpaceDb.dbStorage.clearRowsBefore(clearBefore);
		app.deviceSpaceDb.dbMemory.clearRowsBefore(clearBefore);
		app.deviceSystemDb.dbDateTimeOffsets.clearRowsBefore(clearBefore);

		return 1;
	}



	public JSONArray getMomentaryConcatSystemMetaValuesAsJsonArray(String metaTag) {

		JSONArray metaJsonArray = new JSONArray();

		try {
			JSONObject metaJson = new JSONObject();

			if ("storage".equalsIgnoreCase(metaTag)) {
				long[] storageStats = DeviceStorage.getCurrentStorageStats();
				metaJson.put("internal", "internal*" + storageStats[0] + "*" + storageStats[1] + "*" + storageStats[2]);
				metaJson.put("external", "external*" + storageStats[0] + "*" + storageStats[3] + "*" + storageStats[4]);

			} else if ("memory".equalsIgnoreCase(metaTag)) {
				long[] memoryStats = DeviceMemory.getCurrentMemoryStats(this.context);
				metaJson.put("memory", memoryStats[0] + "*" + memoryStats[1] + "*" + memoryStats[2] + "*" + memoryStats[3] );

			} else if ("cpu".equalsIgnoreCase(metaTag)) {
				int[] cpuStats = app.deviceCPU.getCurrentValues();
				metaJson.put("cpu",System.currentTimeMillis() + "*" + cpuStats[0] + "*" + cpuStats[1] + "*" + cpuStats[2]);

			} else if ("network".equalsIgnoreCase(metaTag)) {
				String[] networkStats = app.deviceMobileNetwork.getMobileNetworkSummary();
				if ((networkStats[2] != null) && (networkStats[3] != null)) {
					metaJson.put("network", networkStats[0] + "*" + networkStats[1] + "*" + networkStats[2] + "*" + networkStats[3]);
				}
			}

			metaJsonArray.put(metaJson);

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);

		} finally {
			return metaJsonArray;
		}

	}


	public void checkReportMobileNetworkChange(List<String[]> cachedValsList, String[] latestVal) {

		String[] logVal = new String[] {};

		if (cachedValsList.size() == 0) {
			logVal = latestVal;
		} else {
			String[] lastCachedVals = cachedValsList.get(cachedValsList.size()-1);
			if ( !lastCachedVals[1].equalsIgnoreCase(latestVal[1]) || !lastCachedVals[2].equalsIgnoreCase(latestVal[2]) || !lastCachedVals[3].equalsIgnoreCase(latestVal[3]) ) {
				logVal = lastCachedVals;
			}
		}

		if (logVal.length > 0) {
			Log.d(logTag, "Mobile Network at "+DateTimeUtils.getDateTime()+" [ name: "+latestVal[3]+", type: "+latestVal[2]+", signal: "+latestVal[1]+" dBm ]");
		}

	}

}
