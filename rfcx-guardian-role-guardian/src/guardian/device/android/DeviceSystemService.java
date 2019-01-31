package guardian.device.android;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rfcx.utility.device.DeviceMobileNetwork;
import rfcx.utility.misc.ArrayUtils;
import rfcx.utility.rfcx.RfcxLog;

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
import guardian.RfcxGuardian;

public class DeviceSystemService extends Service implements SensorEventListener, LocationListener {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceSystemService.class);
	
	private static final String SERVICE_NAME = "DeviceSystem";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceSystemSvc deviceSystemSvc;
	
	private int referenceCycleDuration = 1; 
	
	private int innerLoopIncrement = 0;
	private int innerLoopsPerCaptureCycle = 1;
	private long innerLoopDelayRemainderInMilliseconds = 0;
	
	private int outerLoopIncrement = 0;
	private int outerLoopCaptureCount = 0;

	private SignalStrengthListener signalStrengthListener;
	private TelephonyManager telephonyManager;
	private SignalStrength telephonySignalStrength;
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
	private List<int[]> cpuUsageValues = new ArrayList<int[]>();
	private List<double[]> accelSensorValues = new ArrayList<double[]>();
	private List<double[]> geoPositionValues = new ArrayList<double[]>();
	
	private boolean isListenerRegistered_telephony = false;
	private boolean isListenerRegistered_light = false;
	private boolean isListenerRegistered_accel = false;
	private boolean isListenerRegistered_geoposition = false;

	private boolean allowListenerRegistration_telephony = true;
	private boolean allowListenerRegistration_light = true;
	private boolean allowListenerRegistration_accel = true;
	private boolean allowListenerRegistration_geoposition = true;
	private boolean allowListenerRegistration_geoposition_gps = true;
	private boolean allowListenerRegistration_geoposition_network = true;
	
	
	private void checkSetSensorManager() {
		if (this.sensorManager == null) { 
			this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
		}
	}
	
	private void checkSetLocationManager() {
		if (this.locationManager == null) { 
			this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE); 
			this.allowListenerRegistration_geoposition_gps = this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			this.allowListenerRegistration_geoposition_network = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			if (this.allowListenerRegistration_geoposition_gps) {
				this.geoPositionProviderInfo = LocationManager.GPS_PROVIDER;
				Log.d(logTag, "GeoPosition will be provided via GPS.");
			} else if (this.allowListenerRegistration_geoposition_network) {
				this.geoPositionProviderInfo = LocationManager.NETWORK_PROVIDER;
				Log.d(logTag, "GeoPosition will be provided via Mobile Network.");
			}
		}
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
		Log.v(logTag, "Starting service: "+logTag);
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
					
					confirmOrSetSystemCaptureParameters();
					
					// Sample CPU Stats
					app.deviceCPU.update();
					
					// Sample Inner Loop Stats (Accelerometer)
					innerLoopIncrement = triggerOrSkipInnerLoopSensorMeasurement(innerLoopIncrement, innerLoopsPerCaptureCycle);
					
					if (innerLoopIncrement < innerLoopsPerCaptureCycle) {
						
						Thread.sleep(innerLoopDelayRemainderInMilliseconds);
						
					} else {

						app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

						// Sample Outer Loop Stats (GeoPosition)
						outerLoopIncrement = triggerOrSkipOuterLoopSensorMeasurements(outerLoopIncrement, outerLoopCaptureCount);
						
						// capture and cache cpu usage info
						cpuUsageValues.add(app.deviceCPU.getCurrentStats());
						saveSnapshotValuesToDatabase("cpu");
						
						// capture and cache light sensor data
						cacheSnapshotValues("light", new double[] { lightSensorLastValue } );
						saveSnapshotValuesToDatabase("light");
						
						// capture and cache telephony signal strength
						cacheSnapshotValues("telephony", new double[]{});
						saveSnapshotValuesToDatabase("telephony");
						
						// capture and cache data transfer info
						dataTransferValues.add(app.deviceNetworkStats.getDataTransferStatsSnapshot());
						saveSnapshotValuesToDatabase("datatransfer");

						// capture and cache battery level info
						batteryLevelValues.add(app.deviceBattery.getBatteryState(app.getApplicationContext(), null));
						saveSnapshotValuesToDatabase("battery"); 

						// cache accelerometer sensor data
						saveSnapshotValuesToDatabase("accel");
					}
					
				} catch (InterruptedException e) {
					deviceSystemService.runFlag = false;
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					RfcxLog.logExc(logTag, e);
				}
			}
			Log.v(logTag, "Stopping service: "+logTag);
		}		
	}
	
	
	private boolean confirmOrSetSystemCaptureParameters() {
		
		if (app != null) {

			if (innerLoopIncrement == 0) {
				
				boolean limitBasedOnBatteryLevel = app.audioCaptureUtils.limitBasedOnBatteryLevel();
				int audioCycleDuration = app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
				
				int prefsReferenceCycleDuration = limitBasedOnBatteryLevel ? ( 2 * audioCycleDuration ) : audioCycleDuration;
				
				if (this.referenceCycleDuration != prefsReferenceCycleDuration) {
			
					this.referenceCycleDuration = prefsReferenceCycleDuration;
					this.innerLoopsPerCaptureCycle = DeviceUtils.getInnerLoopsPerCaptureCycle(prefsReferenceCycleDuration);
					this.outerLoopCaptureCount = DeviceUtils.getOuterLoopCaptureCount(prefsReferenceCycleDuration);
					app.deviceCPU.setReportingSampleCount(this.innerLoopsPerCaptureCycle);
					this.innerLoopDelayRemainderInMilliseconds = DeviceUtils.getInnerLoopDelayRemainder(prefsReferenceCycleDuration);
					
					Log.d(logTag, (new StringBuilder())
							.append("SystemStats Capture").append(limitBasedOnBatteryLevel ? " (low power mode)" : "").append(": ")
							.append("Snapshots (all metrics) taken every ").append(Math.round(DeviceUtils.getCaptureCycleDuration(prefsReferenceCycleDuration)/1000)).append(" seconds.")
							.toString());
				}
			}
			
		} else {
			return false;
		}
		
		return true;
	}
	
	private int triggerOrSkipInnerLoopSensorMeasurement(int innerLoopIncrement, int innerLoopsPerCaptureCycle) {
		
		innerLoopIncrement++;
		if (innerLoopIncrement > innerLoopsPerCaptureCycle) { innerLoopIncrement = 0; }

		int halfLoopsBetweenAccelSensorToggle = Math.round( innerLoopsPerCaptureCycle / ( DeviceUtils.accelSensorSnapshotsPerCaptureCycle * 2 ) );
		
		for (int i = 0; i < ( DeviceUtils.accelSensorSnapshotsPerCaptureCycle * 2 ); i++) {
			if (innerLoopIncrement == (i * 2 * halfLoopsBetweenAccelSensorToggle)) { 
				registerListener("accel");
				break;
			} else if (innerLoopIncrement == (i * halfLoopsBetweenAccelSensorToggle)) {
				unRegisterListener("accel");
				break;
			}
		}
		
		return innerLoopIncrement;
		
	}
	
	private int triggerOrSkipOuterLoopSensorMeasurements(int outerLoopIncrement, int outerLoopCaptureCount) {
		
		outerLoopIncrement++;
		
		if (outerLoopIncrement >= outerLoopCaptureCount) { outerLoopIncrement = 0; }
		
		if (outerLoopIncrement == 1) {			
		//	Log.e(logTag, "RUNNING OUTER LOOP LOGIC...");

		} else {

		}
		
		return outerLoopIncrement;
	}
	
	
	private void cacheSnapshotValues(String sensorAbbrev, double[] vals) {
		
		if (sensorAbbrev.equalsIgnoreCase("accel")) {
			
			if (vals.length >= 3) {
				double[] accelArray = new double[] { (double) System.currentTimeMillis(), vals[0], vals[1], vals[2], 0 };
				
				this.accelSensorValues.add(accelArray);
				if (this.app != null) { this.app.deviceUtils.addAccelSensorSnapshotEntry(accelArray); }
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("light")) {
			
			this.lightSensorLastValue = vals[0];
			long lightSensorLastValueAsLong = (long) Math.round(this.lightSensorLastValue);
			if (		(this.lightSensorLastValue != Float.MAX_VALUE)
				&&	(	(this.lightSensorValues.size() == 0) 
					|| 	(lightSensorLastValueAsLong != this.lightSensorValues.get(this.lightSensorValues.size()-1)[1])
					)
			) {
				this.lightSensorValues.add( new long[] { System.currentTimeMillis(), lightSensorLastValueAsLong } );
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("telephony")) {
			
			if ((this.telephonyManager != null) && (this.telephonySignalStrength != null)) {
				this.telephonyValues.add(DeviceMobileNetwork.getMobileNetworkSummary(this.telephonyManager, this.telephonySignalStrength));
			}
			
		} else {
			Log.e(logTag, "Snapshot could not be cached for '"+sensorAbbrev+"'.");
		}
		
		
	}
		
	
	private void registerListener(String sensorAbbrev) {
		
		if (sensorAbbrev.equalsIgnoreCase("accel") && this.allowListenerRegistration_accel) {
			if (!this.isListenerRegistered_accel) {
				checkSetSensorManager();
				if (this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
					this.accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
					this.sensorManager.registerListener(this, this.accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
					this.isListenerRegistered_accel = true;
				} else {
					this.allowListenerRegistration_accel = false;
					Log.d(logTag, "Disabling Listener Registration for Accelerometer because it doesn't seem to be present.");
				}
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("light") && this.allowListenerRegistration_light) { 
			if (!this.isListenerRegistered_light) {
				checkSetSensorManager();
				if (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0) {
					this.lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
					this.sensorManager.registerListener(this, this.lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
					this.isListenerRegistered_light = true;
				} else {
					this.allowListenerRegistration_light = false;
					Log.d(logTag, "Disabling Listener Registration for LightMeter because it doesn't seem to be present.");
				}
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("telephony") && this.allowListenerRegistration_telephony) {
			if (!this.isListenerRegistered_telephony) {
				this.signalStrengthListener = new SignalStrengthListener();
				this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
				this.isListenerRegistered_telephony = true;
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("geoposition") && this.allowListenerRegistration_geoposition) {
			if (!this.isListenerRegistered_geoposition) {
				checkSetLocationManager();
				if (!this.geoPositionProviderInfo.isEmpty()) {
					this.locationManager.requestLocationUpdates(
									this.geoPositionProviderInfo, 
									DeviceUtils.geoPositionMinTimeElapsedBetweenUpdatesInSeconds[app.deviceUtils.geoPositionUpdateIndex] * 1000, 
									DeviceUtils.geoPositionMinDistanceChangeBetweenUpdatesInMeters[app.deviceUtils.geoPositionUpdateIndex], 
									this);
					this.isListenerRegistered_geoposition = true;
				} else {
					Log.e(logTag, "Couldn't register geoposition listener...");
				}
			}
			
		} else {
			Log.e(logTag, "Listener failed to register for '"+sensorAbbrev+"'.");
		}
		
	}
	
	private void unRegisterListener(String sensorAbbrev) {
		
		if (sensorAbbrev.equalsIgnoreCase("accel")) { 
			if (this.isListenerRegistered_accel && (this.accelSensor != null)) {
				this.sensorManager.unregisterListener(this, this.accelSensor);
				this.isListenerRegistered_accel = false;
				if (this.app != null) { this.app.deviceUtils.processAccelSensorSnapshot(); }
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("light")) { 
			if (this.isListenerRegistered_light && (this.lightSensor != null)) {
				this.sensorManager.unregisterListener(this, this.lightSensor); 
				this.isListenerRegistered_light = false;
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("telephony")) { 
			if (this.isListenerRegistered_telephony && (this.telephonyManager != null)) {
				this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_NONE); 
				this.isListenerRegistered_telephony = false;
			}
			
		} else if (sensorAbbrev.equalsIgnoreCase("geoposition")) { 
			if (this.isListenerRegistered_geoposition && (this.locationManager != null)) {
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
					app.deviceSystemDb.dbCPU.insert(new Date(), cpuVals[0], cpuVals[1]);
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
				
				for (String[] telephonyVals : telephonyValuesCache) {
					app.deviceSystemDb.dbTelephony.insert(new Date((long) Long.parseLong(telephonyVals[0])), (int) Integer.parseInt(telephonyVals[1]), telephonyVals[2], telephonyVals[3]);
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
					app.deviceSystemDb.dbBattery.insert(new Date(), batteryLevelVals[0], batteryLevelVals[1]);
					app.deviceSystemDb.dbPower.insert(new Date(), batteryLevelVals[2], batteryLevelVals[3]);
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
			telephonySignalStrength = signalStrength;
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
		if (app != null) {
			app.deviceUtils.processAndSaveGeoPosition(location);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
}
