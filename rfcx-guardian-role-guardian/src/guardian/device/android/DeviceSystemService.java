package guardian.device.android;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rfcx.utility.device.DeviceMobileNetwork;
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

	private SignalStrengthListener signalStrengthListener;
	private TelephonyManager telephonyManager;
	private SignalStrength telephonySignalStrength;
	
	private SensorManager sensorManager;
	private Sensor lightSensor;
	private Sensor accelSensor;
	
	private float lightSensorLastValue = Float.MAX_VALUE;

	private List<String[]> telephonyValues = new ArrayList<String[]>();
	private List<long[]> lightSensorValues = new ArrayList<long[]>();
	private List<long[]> dataTransferValues = new ArrayList<long[]>();
	private List<int[]> batteryLevelValues = new ArrayList<int[]>();
	private List<int[]> cpuUsageValues = new ArrayList<int[]>();
	private List<long[]> accelSensorValues = new ArrayList<long[]>();
	private List<double[]> geoLocationValues = new ArrayList<double[]>();
	
	private boolean isListenerRegistered_telephony = false;
	private boolean isListenerRegistered_light = false;
	private boolean isListenerRegistered_accel = false;

	private boolean allowListenerRegistration_telephony = true;
	private boolean allowListenerRegistration_light = true;
	private boolean allowListenerRegistration_accel = true;
	
	
	
	
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
		return START_STICKY;
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
	}
	
	
	private class DeviceSystemSvc extends Thread {
		
		public DeviceSystemSvc() {
			super("DeviceSystemService-DeviceSensorSvc");
		}
		
		@Override
		public void run() {
			DeviceSystemService deviceSystemService = DeviceSystemService.this;

			app = (RfcxGuardian) getApplication();

			int loopIncrement = 0;
			int audioCycleDuration = 1;
			int loopsPerCaptureCycle = 1;
			long loopDelayRemainderInMilliseconds = 0;
			
			while (deviceSystemService.runFlag) {
				
				try {
					
					if ( (loopIncrement == 0) && (audioCycleDuration != app.rfcxPrefs.getPrefAsInt("audio_cycle_duration")) ) {
						
						audioCycleDuration = app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
						loopsPerCaptureCycle = DeviceSystemUtils.getLoopsPerCaptureCycle(audioCycleDuration);
						app.deviceCPU.setReportingSampleCount(loopsPerCaptureCycle);
						loopDelayRemainderInMilliseconds = DeviceSystemUtils.getLoopDelayRemainder(audioCycleDuration);
						Log.d(logTag, "SystemStats Capture Params: Snapshots every "+Math.round(DeviceSystemUtils.getCaptureCycleDuration(audioCycleDuration)/1000)+" seconds.");
					}
					
					// Sample CPU Stats
					app.deviceCPU.update();
					
					// Sample Accelerometer Stats
					triggerOrSkipAccelSensorSnapshot(loopIncrement, loopsPerCaptureCycle);
					
					// Increment Recording Loop Counter
					loopIncrement++;
					
					if (loopIncrement < loopsPerCaptureCycle) {
						
						Thread.sleep(loopDelayRemainderInMilliseconds);
						
					} else {

						app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
						loopIncrement = 0;
						
						cpuUsageValues.add(app.deviceCPU.getCurrentStats());
						saveSystemStatValuesToDatabase("cpu");

						saveSystemStatValuesToDatabase("accel");
						
						// capture and cache sensor data
						cacheSnapshotValues("light", new float[] { lightSensorLastValue } );
						saveSystemStatValuesToDatabase("light");
						
						// capture and cache telephony signal strength
						cacheSnapshotValues("telephony", null);
						saveSystemStatValuesToDatabase("telephony");
						
						// capture and cache data transfer stats
						dataTransferValues.add(app.deviceNetworkStats.getDataTransferStatsSnapshot());
						saveSystemStatValuesToDatabase("datatransfer");

						// capture and cache battery level stats
						batteryLevelValues.add(app.deviceBattery.getBatteryState(app.getApplicationContext(), null));
						saveSystemStatValuesToDatabase("battery");
						
						// capture and cache geo location stats
						geoLocationValues.add(app.deviceSystemUtils.getCurrentGeoLocation());
						saveSystemStatValuesToDatabase("geolocation");
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
	
	private void triggerOrSkipAccelSensorSnapshot(int loopIncrement, int loopsPerCaptureCycle) {
		
		int loopsBetweenMeasurements = Math.round(loopsPerCaptureCycle / DeviceSystemUtils.accelSensorSnapshotsPerCaptureCycle);
		int halfLoopsBetweenMeasurements = Math.round(loopsPerCaptureCycle / ( DeviceSystemUtils.accelSensorSnapshotsPerCaptureCycle * 2 ) );
		
		for (int i = 0; i < ( DeviceSystemUtils.accelSensorSnapshotsPerCaptureCycle * 2 ); i++) {
			if (loopIncrement == (i * loopsBetweenMeasurements)) { 
				registerListener("accel");
				break;
			} else if (loopIncrement == (i * halfLoopsBetweenMeasurements)) {
				unRegisterListener("accel");
				break;
			}
		}
	}
	
	
	private void cacheSnapshotValues(String sensorAbbreviation, float[] entryValue) {
		
		if (sensorAbbreviation.equalsIgnoreCase("accel")) {
			
			if (entryValue.length >= 3) {
				long[] accelArray = new long[] { 
						System.currentTimeMillis(), 
						(long) Math.round(entryValue[0]*DeviceSystemUtils.accelSensorValueFloatMultiplier), 
						(long) Math.round(entryValue[1]*DeviceSystemUtils.accelSensorValueFloatMultiplier), 
						(long) Math.round(entryValue[2]*DeviceSystemUtils.accelSensorValueFloatMultiplier) 
					};
				
				this.accelSensorValues.add(accelArray);
				if (this.app != null) { this.app.deviceSystemUtils.addAccelSensorSnapshotEntry(accelArray); }
			}
			
		} else if (sensorAbbreviation.equalsIgnoreCase("light")) {
			
			this.lightSensorLastValue = entryValue[0];
			long lightSensorLastValueAsLong = (long) Math.round(this.lightSensorLastValue);
			if (		(this.lightSensorLastValue != Float.MAX_VALUE)
				&&	(	(this.lightSensorValues.size() == 0) 
					|| 	(lightSensorLastValueAsLong != this.lightSensorValues.get(this.lightSensorValues.size()-1)[1])
					)
			) {
				this.lightSensorValues.add( new long[] { System.currentTimeMillis(), lightSensorLastValueAsLong } );
			}
			
		} else if (sensorAbbreviation.equalsIgnoreCase("telephony")) {
			
			if ((telephonyManager != null) && (telephonySignalStrength != null)) {
				telephonyValues.add(DeviceMobileNetwork.getMobileNetworkSummary(telephonyManager, telephonySignalStrength));
			}
			
		} else {
			Log.e(logTag, "Snapshot could not be cached for '"+sensorAbbreviation+"'.");
		}
		
		
	}
	
	
	private void registerListener(String sensorAbbreviation) {
		
		this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		if (sensorAbbreviation.equalsIgnoreCase("accel") && this.allowListenerRegistration_accel) {
			if (!this.isListenerRegistered_accel) {
				if (this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
					this.accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
					this.sensorManager.registerListener(this, this.accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
					this.isListenerRegistered_accel = true;
				} else {
					this.allowListenerRegistration_accel = false;
					Log.d(logTag, "Disabling Listener Registration for Accelerometer because it doesn't seem to be present.");
				}
			}
			
		} else if (sensorAbbreviation.equalsIgnoreCase("light") && this.allowListenerRegistration_light) { 
			if (!this.isListenerRegistered_light) {
				if (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0) {
					this.lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
					this.sensorManager.registerListener(this, this.lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
					this.isListenerRegistered_light = true;
				} else {
					this.allowListenerRegistration_light = false;
					Log.d(logTag, "Disabling Listener Registration for LightMeter because it doesn't seem to be present.");
				}
			}
			
		} else if (sensorAbbreviation.equalsIgnoreCase("telephony") && this.allowListenerRegistration_telephony) {
			if (!this.isListenerRegistered_telephony) {
				this.signalStrengthListener = new SignalStrengthListener();
				this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
				this.isListenerRegistered_telephony = true;
			}
			
		} else {
			Log.e(logTag, "Listener failed to register for '"+sensorAbbreviation+"'.");
		}
		
	}
	
	private void unRegisterListener(String sensorAbbreviation) {
		
		if (sensorAbbreviation.equalsIgnoreCase("accel") && (this.accelSensor != null)) { 
			if (this.isListenerRegistered_accel) {
				this.sensorManager.unregisterListener(this, this.accelSensor);
				this.isListenerRegistered_accel = false;
				if (this.app != null) { this.app.deviceSystemUtils.processAccelSensorSnapshot(); }
			}
			
		} else if (sensorAbbreviation.equalsIgnoreCase("light") && (this.lightSensor != null)) { 
			if (this.isListenerRegistered_light) {
				this.sensorManager.unregisterListener(this, this.lightSensor); 
				this.isListenerRegistered_light = false;
			}
			
		} else if (sensorAbbreviation.equalsIgnoreCase("telephony") && (this.telephonyManager != null)) { 
			if (this.isListenerRegistered_telephony) {
				this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_NONE); 
				this.isListenerRegistered_telephony = false;
			}
			
		} else {
			Log.e(logTag, "Listener failed to unregister for '"+sensorAbbreviation+"'.");
		}
	}
	
	private void saveSystemStatValuesToDatabase(String statAbbreviation) {
		
		try {
			
			if (statAbbreviation.equalsIgnoreCase("cpu")) {
				
				List<int[]> cpuUsageValuesCache = this.cpuUsageValues;
				this.cpuUsageValues = new ArrayList<int[]>();
				
				for (int[] cpuVals : cpuUsageValuesCache) {
					app.deviceSystemDb.dbCPU.insert(new Date(), cpuVals[0], cpuVals[1]);
				}
				
			} else if (statAbbreviation.equalsIgnoreCase("light")) {
				
				List<long[]> lightSensorValuesCache = this.lightSensorValues;
				this.lightSensorValues = new ArrayList<long[]>();
				
				for (long[] lightVals : lightSensorValuesCache) {
					app.deviceSensorDb.dbLightMeter.insert(new Date(lightVals[0]), lightVals[1], "");
				}
				
			} else if (statAbbreviation.equalsIgnoreCase("accel")) {
				
				List<long[]> accelSensorValuesCache = this.accelSensorValues;
				this.accelSensorValues = new ArrayList<long[]>();
				
				double[] accelSensorAverages = DeviceSystemUtils.generateAverageAccelValue(accelSensorValuesCache);
				
				if (accelSensorAverages[4] > 0) {
					app.deviceSensorDb.dbAccelerometer.insert(
							new Date((long) Math.round(accelSensorAverages[0])), 
							TextUtils.join(",", new String[] { accelSensorAverages[1]+"", accelSensorAverages[2]+"", accelSensorAverages[3]+"" }),
							(int) Math.round(accelSensorAverages[4])
						);
				}
					
			} else if (statAbbreviation.equalsIgnoreCase("telephony")) {
				
				List<String[]> telephonyValuesCache = this.telephonyValues;
				this.telephonyValues = new ArrayList<String[]>();
				
				for (String[] telephonyVals : telephonyValuesCache) {
					app.deviceSystemDb.dbTelephony.insert(new Date((long) Long.parseLong(telephonyVals[0])), (int) Integer.parseInt(telephonyVals[1]), telephonyVals[2], telephonyVals[3]);
				}

			} else if (statAbbreviation.equalsIgnoreCase("datatransfer")) {
			
				List<long[]> dataTransferValuesCache = this.dataTransferValues;
				this.dataTransferValues = new ArrayList<long[]>();
				
				for (long[] dataTransferVals : dataTransferValuesCache) {
					// before saving, make sure this isn't the first time the stats are being generated (that throws off the net change figures)
					if (dataTransferVals[6] == 0) {
						app.deviceDataTransferDb.dbTransferred.insert(new Date(), new Date(dataTransferVals[0]), new Date(dataTransferVals[1]), dataTransferVals[2], dataTransferVals[3], dataTransferVals[4], dataTransferVals[5]);
					}
				}
				
			} else if (statAbbreviation.equalsIgnoreCase("battery")) {
				
				List<int[]> batteryLevelValuesCache = this.batteryLevelValues;
				this.batteryLevelValues = new ArrayList<int[]>();
				
				for (int[] batteryLevelVals : batteryLevelValuesCache) {
					app.deviceSystemDb.dbBattery.insert(new Date(), batteryLevelVals[0], batteryLevelVals[1]);
					app.deviceSystemDb.dbPower.insert(new Date(), batteryLevelVals[2], batteryLevelVals[3]);
				}
				
			} else if (statAbbreviation.equalsIgnoreCase("geolocation")) {
				
				List<double[]> geoLocationValuesCache = this.geoLocationValues;
				this.geoLocationValues = new ArrayList<double[]>();
				
				for (double[] geoLocationVals : geoLocationValuesCache) {
					app.deviceSensorDb.dbGeoLocation.insert(new Date((long) Math.round(geoLocationVals[0])), geoLocationVals[1], geoLocationVals[2], geoLocationVals[3]);
				}
				
			} else {
				Log.e(logTag, "Value info for '"+statAbbreviation+"' could not be saved to database.");
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
			cacheSnapshotValues("telephony", null);
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
			
			cacheSnapshotValues("light", event.values);

		} else if (eventType == Sensor.TYPE_ACCELEROMETER) {
			
			cacheSnapshotValues("accel", event.values);
			if (this.isListenerRegistered_accel) { unRegisterListener("accel"); }
			
		}
	}
	
	
/*
 *  These are methods for LocationListener
 */
	
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		
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
