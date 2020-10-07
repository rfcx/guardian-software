package org.rfcx.guardian.admin.device.android.system;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.capture.DeviceMemory;
import org.rfcx.guardian.utility.device.capture.DeviceMobileNetwork;
import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DeviceUtils {

	public DeviceUtils(Context context) {
		this.context = context;
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DeviceUtils");

	private Context context;

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
			return this.allowListenerRegistration_geoposition && ((RfcxGuardian) this.context.getApplicationContext()).rfcxPrefs.getPrefAsBoolean("admin_enable_geoposition_capture");
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

	public static final long captureLoopIncrementFullDurationInMilliseconds = 1000;
	public static final long captureCycleMinimumAllowedDurationInMilliseconds = 30000;
	public static final double captureCycleDurationRatioComparedToAudioCycleDuration = 0.66666667;

	public static final int inReducedCaptureModeExtendCaptureCycleByFactorOf = 3;

	public static final long geoPositionMinDistanceChangeBetweenUpdatesInMeters = 1;

	private List<double[]> accelSensorSnapshotValues = new ArrayList<double[]>();
	public static final int accelSensorSnapshotsPerCaptureCycle = 2;

	private List<double[]> recentValuesAccelSensor = new ArrayList<double[]>();
	private List<double[]> recentValuesGeoLocation = new ArrayList<double[]>();


	public static boolean isReducedCaptureModeActive(String activityTag, Context context) {
		try {
			if (((RfcxGuardian) context.getApplicationContext()).isGuardianRegistered()) {
				JSONArray jsonArray = RfcxComm.getQueryContentProvider("guardian", "status", "*", context.getContentResolver());
				if (jsonArray.length() > 0) {
					JSONObject jsonObj = jsonArray.getJSONObject(0);
					if (jsonObj.has(activityTag)) {
						JSONObject audioCaptureObj = jsonObj.getJSONObject(activityTag);
						if (audioCaptureObj.has("is_allowed") && audioCaptureObj.has("is_disabled")) {
							return !audioCaptureObj.getBoolean("is_allowed") || audioCaptureObj.getBoolean("is_disabled");
						}
					}
				}
			}
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return true;
	}

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
	
	public static long getInnerLoopDelayRemainder(int audioCycleDurationInSeconds, double captureCycleDurationPercentageMultiplier, long samplingOperationDuration) {
		return (long) ( Math.round( ( getCaptureCycleDuration(audioCycleDurationInSeconds) / getInnerLoopsPerCaptureCycle(audioCycleDurationInSeconds) ) - samplingOperationDuration ) * captureCycleDurationPercentageMultiplier );
	}
	
	public static int getOuterLoopCaptureCount(int audioCycleDurationInSeconds) {
//		return (int) ( Math.round( geoPositionMinTimeElapsedBetweenUpdatesInSeconds[0] / ( getCaptureCycleDuration(audioCycleDurationInSeconds) / 1000 ) ) );
		return 3;
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

			RfcxGuardian app = (RfcxGuardian) this.context.getApplicationContext();

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

				RfcxGuardian app = (RfcxGuardian) this.context.getApplicationContext();

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
				int[] cpuStats = ((RfcxGuardian) this.context.getApplicationContext()).deviceCPU.getCurrentStats();
				metaJson.put("cpu",System.currentTimeMillis() + "*" + cpuStats[0] + "*" + cpuStats[1]);

			} else if ("network".equalsIgnoreCase(metaTag)) {
				String[] networkStats = ((RfcxGuardian) this.context.getApplicationContext()).deviceMobileNetwork.getMobileNetworkSummary();
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

}
