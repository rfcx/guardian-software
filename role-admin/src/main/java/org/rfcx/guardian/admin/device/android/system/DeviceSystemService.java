package org.rfcx.guardian.admin.device.android.system;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.capture.DeviceCPU;
import org.rfcx.guardian.utility.device.capture.DeviceMemory;
import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DeviceSystemService extends Service implements SensorEventListener, LocationListener {

	public static final String SERVICE_NAME = "DeviceSystem";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DeviceSystemService");

	private RfcxGuardian app;

	private boolean runFlag = false;
	private DeviceSystemSvc deviceSystemSvc;

	private int referenceCycleDuration = 1;

	private int innerLoopIncrement = 1;
	private int innerLoopsPerCaptureCycle = 1;
	private long innerLoopDelayRemainderInMilliseconds = 0;

	// Sampling adds to the duration of the overall capture cycle, so we cut it short slightly based on an EMPIRICALLY DETERMINED percentage
	// This can help ensure, for example, that a 60 second capture loop actually returns values with an interval of 60 seconds, instead of 61 or 62 seconds
	private double captureCycleLastDurationPercentageMultiplier = 0.98;
	private long captureCycleLastStartTime = 0;
	private long[] captureCycleMeasuredDurations = new long[] { 0, 0, 0 };
	private double[] captureCyclePercentageMultipliers = new double[] { 0, 0, 0 };

	private int outerLoopIncrement = 0;
	private int outerLoopCaptureCount = 0;

	private boolean isReducedCaptureModeActive = false;

	private SignalStrengthListener signalStrengthListener;

	private LocationManager locationManager;
	private SensorManager sensorManager;

	private Sensor lightSensor;
	private Sensor accelSensor;

	private String geoPositionProviderInfo;

	private double lightSensorLastValue = Float.MAX_VALUE;

	private List<String[]> telephonyValues = new ArrayList<String[]>();
	private List<long[]> lightSensorValues = new ArrayList<long[]>();
	private List<long[]> dataTransferValues = new ArrayList<long[]>();
	private List<int[]> batteryLevelValues = new ArrayList<int[]>();
	private List<long[]> storageValues = new ArrayList<long[]>();
	private List<long[]> memoryValues = new ArrayList<long[]>();
	private List<int[]> cpuUsageValues = new ArrayList<int[]>();
	private List<double[]> accelSensorValues = new ArrayList<double[]>();

	private boolean isListenerRegistered_telephony = false;
	private boolean isListenerRegistered_light = false;
	private boolean isListenerRegistered_accel = false;
	private boolean isListenerRegistered_geoposition = false;

	private void checkSetSensorManager() {
		if (this.sensorManager == null) {
			this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		}
	}

	private boolean checkSetLocationManager() {
		boolean isGeoPositionAccessApproved = false;
		if (DeviceUtils.isAppRoleApprovedForGeoPositionAccess(app.getApplicationContext())) {
			if (this.locationManager == null) {
				this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
				app.deviceUtils.allowOrDisableSensorListener("geoposition_gps", this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
				app.deviceUtils.allowOrDisableSensorListener("geoposition_network", this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
				if (app.deviceUtils.isSensorListenerAllowed("geoposition_gps")) {
					this.geoPositionProviderInfo = LocationManager.GPS_PROVIDER;
					Log.d(logTag, "GeoPosition will be provided via GPS.");
					isGeoPositionAccessApproved = true;
				} else if (app.deviceUtils.isSensorListenerAllowed("geoposition_network")) {
					this.geoPositionProviderInfo = LocationManager.NETWORK_PROVIDER;
					Log.d(logTag, "GeoPosition will be provided via Mobile Network.");
					isGeoPositionAccessApproved = true;
				}
			} else {
				isGeoPositionAccessApproved = true;
			}
		} else {
			Log.w(logTag, "This app does not have permissions to access location updates...");
		}
		return isGeoPositionAccessApproved;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceSystemSvc = new DeviceSystemSvc();
		app = (RfcxGuardian) getApplication();

		registerListener("light");
		registerListener("telephony");
		registerListener("geoposition");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: " + logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceSystemSvc.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.deviceSystemSvc.interrupt();
		this.deviceSystemSvc = null;

		unRegisterListener("light");
		unRegisterListener("telephony");
		unRegisterListener("geoposition");
		unRegisterListener("accel");
	}


	private class DeviceSystemSvc extends Thread {

		public DeviceSystemSvc() {
			super("DeviceSystemService-DeviceSensorSvc");
		}

		@Override
		public void run() {
			DeviceSystemService deviceSystemService = DeviceSystemService.this;

			app = (RfcxGuardian) getApplication();

			while (deviceSystemService.runFlag) {

				try {

					confirmOrSetCaptureParameters();

					if (innerLoopDelayRemainderInMilliseconds > 0) {
						Thread.sleep(innerLoopDelayRemainderInMilliseconds);
					}

					// Sample CPU Stats
					app.deviceCPU.update();

					// Inner Loop Behavior
					innerLoopIncrement = triggerOrSkipInnerLoopBehavior(innerLoopIncrement, innerLoopsPerCaptureCycle);

					if (innerLoopIncrement == innerLoopsPerCaptureCycle) {

						app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

						// Outer Loop Behavior
						outerLoopIncrement = triggerOrSkipOuterLoopBehavior(outerLoopIncrement, outerLoopCaptureCount);

					}

				} catch (InterruptedException e) {
					deviceSystemService.runFlag = false;
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					RfcxLog.logExc(logTag, e);
				}
			}
			Log.v(logTag, "Stopping service: " + logTag);
		}
	}


	private boolean confirmOrSetCaptureParameters() {

		if ((app != null) && (innerLoopIncrement == 1)) {

			this.captureCycleLastStartTime = System.currentTimeMillis();

			int audioCycleDuration = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION);

			// when audio capture is disabled (for any number of reasons), we continue to capture system stats...
			// however, we slow the capture cycle by the multiple indicated in DeviceUtils.inReducedCaptureModeExtendCaptureCycleByFactorOf
			int prefsReferenceCycleDuration = this.isReducedCaptureModeActive ? (audioCycleDuration * DeviceUtils.inReducedCaptureModeExtendCaptureCycleByFactorOf) : audioCycleDuration;

			if (this.referenceCycleDuration != prefsReferenceCycleDuration) {

				this.referenceCycleDuration = prefsReferenceCycleDuration;
				this.innerLoopsPerCaptureCycle = DeviceUtils.getInnerLoopsPerCaptureCycle(prefsReferenceCycleDuration);
				this.outerLoopCaptureCount = DeviceUtils.getOuterLoopCaptureCount(prefsReferenceCycleDuration);
				app.deviceCPU.setReportingSampleCount(this.innerLoopsPerCaptureCycle);
				long samplingOperationDuration = DeviceCPU.SAMPLE_DURATION_MILLISECONDS;
				this.innerLoopDelayRemainderInMilliseconds = DeviceUtils.getInnerLoopDelayRemainder(prefsReferenceCycleDuration, this.captureCycleLastDurationPercentageMultiplier, samplingOperationDuration);

				Log.d(logTag, "SystemStats Capture" + (this.isReducedCaptureModeActive ? " (currently limited)" : "") + ": " +
						"Snapshots (all metrics) taken every " + Math.round(DeviceUtils.getCaptureCycleDuration(prefsReferenceCycleDuration) / 1000) + " seconds.");
			}

		} else {
			return false;
		}

		return true;
	}


	private int triggerOrSkipInnerLoopBehavior(int innerLoopIncrement, int innerLoopsPerCaptureCycle) {

		innerLoopIncrement++;
		if (innerLoopIncrement > innerLoopsPerCaptureCycle) {
			innerLoopIncrement = 1;
		}

		if (app.deviceUtils.isSensorListenerAllowed("accel")) {
			int halfLoopsBetweenAccelSensorToggle = Math.round(innerLoopsPerCaptureCycle / (DeviceUtils.accelSensorSnapshotsPerCaptureCycle * 2));
			for (int i = 0; i < (DeviceUtils.accelSensorSnapshotsPerCaptureCycle * 2); i++) {
				if (innerLoopIncrement == (i * 2 * halfLoopsBetweenAccelSensorToggle)) {
					registerListener("accel");
					break;
				} else if (innerLoopIncrement == (i * halfLoopsBetweenAccelSensorToggle)) {
					unRegisterListener("accel");
					break;
				}
			}
		}

		return innerLoopIncrement;
	}

	private int triggerOrSkipOuterLoopBehavior(int outerLoopIncrement, int outerLoopCaptureCount) {

		outerLoopIncrement++;
		if (outerLoopIncrement > outerLoopCaptureCount) {
			outerLoopIncrement = 1;
		}

		setOrUnSetReducedCaptureMode();
		setOrUnSetReducedCaptureModeListeners();

		// capture and cache cpu usage info
		cpuUsageValues.add(app.deviceCPU.getCurrentStats());
		saveSnapshotValuesToDatabase("cpu");

		// capture and cache data transfer info
		dataTransferValues.add(app.deviceNetworkStats.getDataTransferStatsSnapshot());
		saveSnapshotValuesToDatabase("datatransfer");

		// cache accelerometer sensor data
		saveSnapshotValuesToDatabase("accel");

		// capture and cache light sensor data
		cacheSnapshotValues("light", new double[]{ lightSensorLastValue });
		saveSnapshotValuesToDatabase("light");


		if (outerLoopIncrement == outerLoopCaptureCount) {

			// capture and cache battery level info
			batteryLevelValues.add(app.deviceBattery.getBatteryState(app.getApplicationContext(), null));
			saveSnapshotValuesToDatabase("battery");

			// capture and cache storage usage stats
			storageValues.add(DeviceStorage.getCurrentStorageStats());
			saveSnapshotValuesToDatabase("storage");

			// capture and cache memory usage stats
			memoryValues.add(DeviceMemory.getCurrentMemoryStats(app.getApplicationContext()));
			saveSnapshotValuesToDatabase("memory");

			// capture and cache telephony signal strength
			cacheSnapshotValues("telephony", new double[]{});
			saveSnapshotValuesToDatabase("telephony");

		}

		return outerLoopIncrement;
	}


	private void setOrUnSetReducedCaptureMode() {

		this.isReducedCaptureModeActive =
			(	app.sentinelPowerUtils.isReducedCaptureModeActive_BasedOnSentinelPower("audio_capture")
			||	DeviceUtils.isReducedCaptureModeActive("audio_capture", app.getApplicationContext())
			);
	}

	private void setOrUnSetReducedCaptureModeListeners() {

		if (this.isReducedCaptureModeActive) {

			unRegisterListener("geoposition");

		} else {

		//	registerListener("geoposition");

		}
	}


	private void cacheSnapshotValues(String sensorAbbrev, double[] vals) {

		if (sensorAbbrev.equalsIgnoreCase("accel")) {

			if (vals.length >= 3) {
				double[] accelArray = new double[]{(double) System.currentTimeMillis(), vals[0], vals[1], vals[2], 0};

				this.accelSensorValues.add(accelArray);
				if (this.app != null) {
					this.app.deviceUtils.addAccelSensorSnapshotEntry(accelArray);
				}
			}

		} else if (sensorAbbrev.equalsIgnoreCase("light")) {

			this.lightSensorLastValue = vals[0];
			long lightSensorLastValueAsLong = Math.round(this.lightSensorLastValue);
			if (	(this.lightSensorLastValue != Float.MAX_VALUE)
				&& 	(	(this.lightSensorValues.size() == 0)
					|| 	(lightSensorLastValueAsLong != this.lightSensorValues.get(this.lightSensorValues.size() - 1)[1])
					)
			) {
				this.lightSensorValues.add(new long[]{System.currentTimeMillis(), lightSensorLastValueAsLong});
			}

		} else if (sensorAbbrev.equalsIgnoreCase("telephony")) {

			if (app.deviceMobileNetwork.isInitializedTelephonyManager() && app.deviceMobileNetwork.isInitializedSignalStrength()) {
				this.telephonyValues.add(app.deviceMobileNetwork.getMobileNetworkSummary());
			} else {
				Log.e(logTag, "could not cache telephony");
			}

		} else {
			Log.e(logTag, "Snapshot could not be cached for '" + sensorAbbrev + "'.");
		}


	}


	private void registerListener(String sensorAbbrev) {

		try {

			if (sensorAbbrev.equalsIgnoreCase("accel") && app.deviceUtils.isSensorListenerAllowed("accel")) {
				if (!this.isListenerRegistered_accel) {
					checkSetSensorManager();
					if (this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
						Log.v(logTag, "Registering listener for 'accelerometer'...");
						this.accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
						this.sensorManager.registerListener(this, this.accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
						this.isListenerRegistered_accel = true;
					} else {
						app.deviceUtils.disableSensorListener("accel");
						Log.d(logTag, "Disabling Listener Registration for Accelerometer because it doesn't seem to be present.");
					}
				}

			} else if (sensorAbbrev.equalsIgnoreCase("light")
					&& app.deviceUtils.isSensorListenerAllowed("light")
			) {
				if (!this.isListenerRegistered_light) {
					checkSetSensorManager();
					if (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0) {
						Log.v(logTag, "Registering listener for 'light'...");
						this.lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
						this.sensorManager.registerListener(this, this.lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
						this.isListenerRegistered_light = true;
					} else {
						app.deviceUtils.disableSensorListener("light");
						Log.d(logTag, "Disabling Listener Registration for LightMeter because it doesn't seem to be present.");
					}
				}

			} else if (sensorAbbrev.equalsIgnoreCase("telephony") && app.deviceUtils.isSensorListenerAllowed("telephony")) {
				if (!this.isListenerRegistered_telephony) {
					Log.v(logTag, "Registering listener for 'telephony'...");
					this.signalStrengthListener = new SignalStrengthListener();
					app.deviceMobileNetwork.setTelephonyManager((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
					app.deviceMobileNetwork.setTelephonyListener(this.signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
					this.isListenerRegistered_telephony = true;
				}

			} else if (	sensorAbbrev.equalsIgnoreCase("geoposition") && app.deviceUtils.isSensorListenerAllowed("geoposition")) {
				if (!this.isListenerRegistered_geoposition) {
					if (checkSetLocationManager() && !this.geoPositionProviderInfo.isEmpty()) {
						Log.v(logTag, "Registering listener for 'geoposition'...");
						this.locationManager.requestLocationUpdates(
								this.geoPositionProviderInfo,
								( app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.ADMIN_GEOPOSITION_CAPTURE_CYCLE) * 60 * 1000 ),
								DeviceUtils.geoPositionMinDistanceChangeBetweenUpdatesInMeters,
								this);
						this.isListenerRegistered_geoposition = true;
					} else {
						Log.e(logTag, "Couldn't register geoposition listener...");
					}
				}

			} else {
				Log.e(logTag, "No Listener registered for '" + sensorAbbrev + "' sensor...");
			}

		} catch (SecurityException e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	private void unRegisterListener(String sensorAbbrev) {
		
		if (sensorAbbrev.equalsIgnoreCase("accel")) { 
			if (this.isListenerRegistered_accel && (this.accelSensor != null)) {
				Log.v(logTag, "Unregistering sensor listener for 'accelerometer'...");
				this.sensorManager.unregisterListener(this, this.accelSensor);
				this.isListenerRegistered_accel = false;
				if (this.app != null) { this.app.deviceUtils.processAccelSensorSnapshot(); }
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("light")) { 
			if (this.isListenerRegistered_light && (this.lightSensor != null)) {
				Log.v(logTag, "Unregistering sensor listener for 'light'...");
				this.sensorManager.unregisterListener(this, this.lightSensor); 
				this.isListenerRegistered_light = false;
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("telephony")) { 
			if (this.isListenerRegistered_telephony && app.deviceMobileNetwork.isInitializedTelephonyManager()) {
				Log.v(logTag, "Unregistering sensor listener for 'telephony'...");
				app.deviceMobileNetwork.setTelephonyListener(this.signalStrengthListener, PhoneStateListener.LISTEN_NONE);
				this.isListenerRegistered_telephony = false;
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("geoposition")) { 
			if (this.isListenerRegistered_geoposition && (this.locationManager != null)) {
				Log.v(logTag, "Unregistering sensor listener for 'geoposition'...");
				this.locationManager.removeUpdates(this);
				this.isListenerRegistered_geoposition = false;
			}
				 
		} else {
			Log.e(logTag, "Listener failed to unregister for '"+sensorAbbrev+"'.");
		}
	}
	
	private void saveSnapshotValuesToDatabase(String statAbbrev) {
		
		try {
			
			if (statAbbrev.equalsIgnoreCase("cpu")) {
				
				List<int[]> cpuUsageValuesCache = this.cpuUsageValues;
				this.cpuUsageValues = new ArrayList<int[]>();
				
				for (int[] cpuVals : cpuUsageValuesCache) {
					// make sure the values are valid
					if ((cpuVals[0] <= 100) && (cpuVals[0] >= 0)) {
						app.deviceSystemDb.dbCPU.insert(new Date(), cpuVals[0], cpuVals[1]);
					}
				}
				
			} else if (statAbbrev.equalsIgnoreCase("light")) {
				
				List<long[]> lightSensorValuesCache = this.lightSensorValues;
				this.lightSensorValues = new ArrayList<long[]>();
				
				for (long[] lightVals : lightSensorValuesCache) {
					app.deviceSensorDb.dbLightMeter.insert(new Date(lightVals[0]), lightVals[1], "");
				}
				
			} else if (statAbbrev.equalsIgnoreCase("accel")) {
				
				List<double[]> accelSensorValuesCache = this.accelSensorValues;
				this.accelSensorValues = new ArrayList<double[]>();
				
				double[] accelSensorAverages = DeviceUtils.generateAverageAccelValues(accelSensorValuesCache);
				
				if (accelSensorAverages[4] > 0) {
					app.deviceSensorDb.dbAccelerometer.insert(
							new Date((long) Math.round(accelSensorAverages[0])), 
							TextUtils.join(",", new String[] { accelSensorAverages[1]+"", accelSensorAverages[2]+"", accelSensorAverages[3]+"" }),
							(int) Math.round(accelSensorAverages[4])
						);
				}
					
			} else if (statAbbrev.equalsIgnoreCase("telephony")) {
				
				List<String[]> telephonyValuesCache = this.telephonyValues;
				this.telephonyValues = new ArrayList<String[]>();

				String[] prevTelephonyVals = new String[] { "", "", "", "" };

				for (String[] telephonyVals : telephonyValuesCache) {
					if (	((telephonyVals[2] != null) && (telephonyVals[3] != null)) // ensure relevant values aren't just null
						&&	!telephonyVals[1].equalsIgnoreCase(prevTelephonyVals[1])
						&&	!telephonyVals[2].equalsIgnoreCase(prevTelephonyVals[2]) // ensure relevant values aren't just immediate repeats of last saved value
						&& 	!telephonyVals[3].equalsIgnoreCase(prevTelephonyVals[3])
					) {
						app.deviceSystemDb.dbTelephony.insert(new Date(Long.parseLong(telephonyVals[0])), Integer.parseInt(telephonyVals[1]), telephonyVals[2], telephonyVals[3]);
					}
					prevTelephonyVals = telephonyVals;
				}

			} else if (statAbbrev.equalsIgnoreCase("datatransfer")) {
			
				List<long[]> dataTransferValuesCache = this.dataTransferValues;
				this.dataTransferValues = new ArrayList<long[]>();
				
				for (long[] dataTransferVals : dataTransferValuesCache) {
					// before saving, make sure this isn't the first time the stats are being generated (that throws off the net change figures)
					if (dataTransferVals[6] == 0) {
						app.deviceDataTransferDb.dbTransferred.insert(new Date(), new Date(dataTransferVals[0]), new Date(dataTransferVals[1]), dataTransferVals[2], dataTransferVals[3], dataTransferVals[4], dataTransferVals[5]);
					}
				}
				
			} else if (statAbbrev.equalsIgnoreCase("battery")) {
				
				List<int[]> batteryLevelValuesCache = this.batteryLevelValues;
				this.batteryLevelValues = new ArrayList<int[]>();
				
				for (int[] batteryLevelVals : batteryLevelValuesCache) {
					if ((batteryLevelVals[0] <= 100) && (batteryLevelVals[0] >= 0)) {
						app.deviceSystemDb.dbBattery.insert(new Date(),
								app.deviceUtils.allowMeasurement_battery_percentage ? batteryLevelVals[0] : 0,
								app.deviceUtils.allowMeasurement_battery_temperature ? batteryLevelVals[1] : 0,
								app.deviceUtils.allowMeasurement_battery_is_charging ? batteryLevelVals[2] : 0,
								app.deviceUtils.allowMeasurement_battery_is_fully_charged ? batteryLevelVals[3] : 0
							);
					}
				}
				
			} else if (statAbbrev.equalsIgnoreCase("storage")) {
				
				List<long[]> storageValuesCache = this.storageValues;
				this.storageValues = new ArrayList<long[]>();
				
				for (long[] storageVals : storageValuesCache) {
					app.deviceSpaceDb.dbStorage.insert("internal", new Date(storageVals[0]), storageVals[1], storageVals[2]);
					app.deviceSpaceDb.dbStorage.insert("external", new Date(storageVals[0]), storageVals[3], storageVals[4]);
				}

			} else if (statAbbrev.equalsIgnoreCase("memory")) {

				List<long[]> memoryValuesCache = this.memoryValues;
				this.memoryValues = new ArrayList<long[]>();

				for (long[] memoryVals : memoryValuesCache) {
					app.deviceSpaceDb.dbMemory.insert("system", new Date(memoryVals[0]), memoryVals[1], memoryVals[2], memoryVals[3]);
				}
				
			} else {
				Log.e(logTag, "Value info for '"+statAbbrev+"' could not be saved to database.");
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

	
/*
 *  These are methods for PhoneStateListener
 */

	public class SignalStrengthListener extends PhoneStateListener {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);
			app.deviceMobileNetwork.setTelephonySignalStrength(signalStrength);
			cacheSnapshotValues("telephony", new double[]{});
		}
	}



	
/*
 *  These are methods for SensorEventListener
 */
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		
		int eventType = event.sensor.getType();
		
		if (eventType == Sensor.TYPE_LIGHT) {
			
			cacheSnapshotValues("light", ArrayUtils.castFloatArrayToDoubleArray(event.values));

		} else if (eventType == Sensor.TYPE_ACCELEROMETER) {
			
			cacheSnapshotValues("accel", ArrayUtils.castFloatArrayToDoubleArray(event.values));
			if (this.isListenerRegistered_accel) { unRegisterListener("accel"); }
			
		}
	}
	
	
/*
 *  These are methods for LocationListener
 */
	
	@Override
	public void onLocationChanged(Location location) {
	//	Log.e(logTag, "Running onLocationChanged...");
		if (app != null) {
			app.deviceUtils.processAndSaveGeoPosition(location);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	//	Log.e(logTag, "Running onProviderDisabled...");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
	//	Log.e(logTag, "Running onProviderDisabled...");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	//	Log.e(logTag, "Running onStatusChanged...");
		// TODO Auto-generated method stub
		
	}
	
}
