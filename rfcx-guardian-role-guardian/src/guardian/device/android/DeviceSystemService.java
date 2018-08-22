package guardian.device.android;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rfcx.utility.device.DeviceCPU;
import rfcx.utility.device.DeviceMobileNetwork;
import rfcx.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import guardian.RfcxGuardian;

public class DeviceSystemService extends Service implements SensorEventListener {

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

	private List<String[]> telephonyValues = new ArrayList<String[]>();
	private List<long[]> lightSensorValues = new ArrayList<long[]>();
	private List<long[]> accelSensorValues = new ArrayList<long[]>();
	private List<long[]> dataTransferValues = new ArrayList<long[]>();
	private List<int[]> batteryLevelValues = new ArrayList<int[]>();
	private List<int[]> cpuUsageValues = new ArrayList<int[]>();
	
	
	private boolean isListenerRegistered_telephony = false;
	private boolean isListenerRegistered_light = false;
	private boolean isListenerRegistered_accel = false;

	private boolean allowListenerRegistration_telephony = true;
	private boolean allowListenerRegistration_light = true;
	private boolean allowListenerRegistration_accel = true;
	
	private static final int ACCEL_FLOAT_MULTIPLIER = 1000000;
	private static final long CPU_USAGE_MEASUREMENT_LOOP_MS = 1000;
		
	private int cpuUsageRecordingIncrement = 0;
	
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
	}
	
	
	private class DeviceSystemSvc extends Thread {
		
		public DeviceSystemSvc() {
			super("DeviceSystemService-DeviceSensorSvc");
		}
		
		@Override
		public void run() {
			DeviceSystemService deviceSystemService = DeviceSystemService.this;

			app = (RfcxGuardian) getApplication();
			
			long captureCycleDuration = (long) Math.round( app.rfcxPrefs.getPrefAsInt("device_stats_capture_cycle_duration") * 1000 );
			int cpuUsageReportingSampleCount = Math.round( captureCycleDuration / CPU_USAGE_MEASUREMENT_LOOP_MS );
			app.deviceCPU.setReportingSampleCount(cpuUsageReportingSampleCount);
			long cpuUsageCycleDelayRemainderMilliseconds = (long) ( Math.round( captureCycleDuration / cpuUsageReportingSampleCount ) - DeviceCPU.SAMPLE_DURATION_MILLISECONDS );
					
			while (deviceSystemService.runFlag) {
				
				try {
					
					// Sample CPU Stats
					app.deviceCPU.update();
					cpuUsageRecordingIncrement++;
					
					if (cpuUsageRecordingIncrement < cpuUsageReportingSampleCount) {
						
						Thread.sleep(cpuUsageCycleDelayRemainderMilliseconds);
						
						if (cpuUsageRecordingIncrement == Math.round(cpuUsageReportingSampleCount/2)) {
							// quickly toggle accelerometer listener (results to be averaged and saved later in the cycle)
							registerListener("accel");
						}
						
					} else {

						app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

						// cache pre-captured sensor data
						saveSystemStatValuesToDatabase("light");
						saveSystemStatValuesToDatabase("accel");
						
						// capture and cache telephony signal strength
						cacheTelephonySignalStrengthSnapshot();
						saveSystemStatValuesToDatabase("telephony");
						
						// capture and cache data transfer stats
						dataTransferValues.add(app.deviceNetworkStats.getDataTransferStatsSnapshot());
						saveSystemStatValuesToDatabase("datatransfer");

						// capture and cache battery level stats
						batteryLevelValues.add(app.deviceBattery.getBatteryState(app.getApplicationContext(), null));
						saveSystemStatValuesToDatabase("battery");
						
						cpuUsageValues.add(app.deviceCPU.getCurrentStats());
						saveSystemStatValuesToDatabase("cpu");
						cpuUsageRecordingIncrement = 0;
						
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
	
	
	// Sensor methods

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		int eventType = event.sensor.getType();
		
		if (eventType == Sensor.TYPE_LIGHT) {
			
			long lightEventValue = (long) Math.round(event.values[0]);
			int lightEventCacheCount = this.lightSensorValues.size();
			
			if (	(lightEventCacheCount == 0) 
				|| 	(lightEventValue != this.lightSensorValues.get(lightEventCacheCount-1)[1])
				) {
				this.lightSensorValues.add( new long[] { System.currentTimeMillis(), lightEventValue } );	
			}

		} else if (eventType == Sensor.TYPE_ACCELEROMETER) {
			this.accelSensorValues.add( new long[] { 
					System.currentTimeMillis(), 
					(long) Math.round(event.values[0]*ACCEL_FLOAT_MULTIPLIER), 
					(long) Math.round(event.values[1]*ACCEL_FLOAT_MULTIPLIER), 
					(long) Math.round(event.values[2]*ACCEL_FLOAT_MULTIPLIER) 
				} );
			if (this.isListenerRegistered_accel) { unRegisterListener("accel"); }
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}
	
	public class SignalStrengthListener extends PhoneStateListener {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);
			telephonySignalStrength = signalStrength;
			cacheTelephonySignalStrengthSnapshot();
		}
	}
	
	private void cacheTelephonySignalStrengthSnapshot() {
		if ((telephonyManager != null) && (telephonySignalStrength != null)) {
			telephonyValues.add(DeviceMobileNetwork.getMobileNetworkSummary(telephonyManager, telephonySignalStrength));
		}
	}
	
	private void registerListener(String sensorAbbreviation) {
		
		this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		if (sensorAbbreviation.equalsIgnoreCase("accel") && this.allowListenerRegistration_accel) {
			if (this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
				this.accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
				this.sensorManager.registerListener(this, this.accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
				this.isListenerRegistered_accel = true;
			} else {
				this.allowListenerRegistration_accel = false;
				Log.d(logTag, "Disabling Listener Registration for Accelerometer because it doesn't seem to be present.");
			}
			
		} else if (sensorAbbreviation.equalsIgnoreCase("light") && this.allowListenerRegistration_light) { 
			if (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0) {
				this.lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
				this.sensorManager.registerListener(this, this.lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
				this.isListenerRegistered_light = true;
			} else {
				this.allowListenerRegistration_light = false;
				Log.d(logTag, "Disabling Listener Registration for LightMeter because it doesn't seem to be present.");
			}
			
		} else if (sensorAbbreviation.equalsIgnoreCase("telephony") && this.allowListenerRegistration_telephony) {
			this.signalStrengthListener = new SignalStrengthListener();
			this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
			this.isListenerRegistered_telephony = true;
			
		} else {
			Log.e(logTag, "Listener failed to register for '"+sensorAbbreviation+"'.");
		}
		
	}
	
	private void unRegisterListener(String sensorAbbreviation) {
		
		
		if (sensorAbbreviation.equalsIgnoreCase("accel") && (this.accelSensor != null)) { 
			this.isListenerRegistered_accel = false;
			this.sensorManager.unregisterListener(this, this.accelSensor);
			
		} else if (sensorAbbreviation.equalsIgnoreCase("light") && (this.lightSensor != null)) { 
			this.isListenerRegistered_light = false;
			this.sensorManager.unregisterListener(this, this.lightSensor); 
			
		} else if (sensorAbbreviation.equalsIgnoreCase("telephony") && (this.telephonyManager != null)) { 
			this.isListenerRegistered_telephony = false;
			this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_NONE); 
			
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
				
				List<long[]> accelValuesCache = this.accelSensorValues;
				this.accelSensorValues = new ArrayList<long[]>();
				
				long[] avgAccelVals = new long[] { 0, 0, 0, 0 };
				int sampleCount = accelValuesCache.size();
				
				if (sampleCount > 0) {

					for (long[] accelVals : accelValuesCache) {
						avgAccelVals[0] = accelVals[0];
						avgAccelVals[1] = avgAccelVals[1]+accelVals[1];
						avgAccelVals[2] = avgAccelVals[2]+accelVals[2];
						avgAccelVals[3] = avgAccelVals[3]+accelVals[3];
						sampleCount++;
					}
					
					avgAccelVals[1] = (long) Math.round(avgAccelVals[1]/sampleCount);
					avgAccelVals[2] = (long) Math.round(avgAccelVals[2]/sampleCount);
					avgAccelVals[3] = (long) Math.round(avgAccelVals[3]/sampleCount);
		
					app.deviceSensorDb.dbAccelerometer.insert(
							new Date(avgAccelVals[0]), 
							(((double) avgAccelVals[1])/ACCEL_FLOAT_MULTIPLIER)
							+","+(((double) avgAccelVals[2])/ACCEL_FLOAT_MULTIPLIER)
							+","+(((double) avgAccelVals[3])/ACCEL_FLOAT_MULTIPLIER),
							sampleCount);
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
				
			} else {
				Log.e(logTag, "Value info for '"+statAbbreviation+"' could not be saved to database.");
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
}
