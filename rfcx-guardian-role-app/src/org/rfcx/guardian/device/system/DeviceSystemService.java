package org.rfcx.guardian.device.system;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.device.DeviceMobileNetwork;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

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

public class DeviceSystemService extends Service implements SensorEventListener {

	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceSystemService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "DeviceSystem";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceSystemSvc deviceSystemSvc;

	private SignalStrengthListener signalStrengthListener;
	private TelephonyManager telephonyManager;
	
	private SensorManager sensorManager;
	private Sensor lightSensor;
	private Sensor accelSensor;

	// local data lists for queuing captured network data and writing to disk in batches
	private List<String[]> networkValues = new ArrayList<String[]>();
	private List<long[]> lightSensorValues = new ArrayList<long[]>();
	private List<long[]> accelSensorValues = new ArrayList<long[]>();
	
	private boolean isRegistered_network = false;
	private boolean isRegistered_light = false;
	private boolean isRegistered_accel = false;
	
	private static final int ACCEL_FLOAT_MULTIPLIER = 1000000;
	
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
		registerListener("network");
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
		unRegisterListener("network");
	}
	
	
	private class DeviceSystemSvc extends Thread {
		
		public DeviceSystemSvc() {
			super("DeviceSystemService-DeviceSensorSvc");
		}
		
		@Override
		public void run() {
			DeviceSystemService deviceSystemService = DeviceSystemService.this;

			app = (RfcxGuardian) getApplication();
			
			long captureCycleDurationHalf = (long) Math.round(app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") / 4 );
					
			while (deviceSystemService.runFlag) {
				
				try {

					Thread.sleep(captureCycleDurationHalf);
					
					// quickly toggle accelerometer listener (results to be averaged and saved later in the cycle)
					registerListener("accel");
					
					Thread.sleep(captureCycleDurationHalf);

					saveSystemStatValuesToDatabase("light");
					saveSystemStatValuesToDatabase("accel");
					saveSystemStatValuesToDatabase("network");
					
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
				this.lightSensorValues.add( new long[] { (new Date()).getTime(), lightEventValue } );	
			}

		} else if (eventType == Sensor.TYPE_ACCELEROMETER) {
			this.accelSensorValues.add( new long[] { 
					(new Date()).getTime(), 
					(long) Math.round(event.values[0]*ACCEL_FLOAT_MULTIPLIER), 
					(long) Math.round(event.values[1]*ACCEL_FLOAT_MULTIPLIER), 
					(long) Math.round(event.values[2]*ACCEL_FLOAT_MULTIPLIER) 
				} );
			if (this.isRegistered_accel) { unRegisterListener("accel"); }
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
			networkValues.add(DeviceMobileNetwork.getMobileNetworkSummary(telephonyManager, signalStrength));
		}
	}
	
	private void registerListener(String sensorAbbreviation) {
		
		this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		if (sensorAbbreviation.equalsIgnoreCase("accel") && (this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0)) {
			this.accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
			this.sensorManager.registerListener(this, this.accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
			this.isRegistered_accel = true;
			
		} else if (sensorAbbreviation.equalsIgnoreCase("light") && (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0)) {
			this.lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
			this.sensorManager.registerListener(this, this.lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
			this.isRegistered_light = true;
			
		} else if (sensorAbbreviation.equalsIgnoreCase("network")) {
			this.signalStrengthListener = new SignalStrengthListener();
			this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
			this.isRegistered_network = true;
			
		} else {
			Log.e(logTag, "Listener failed to register for '"+sensorAbbreviation+"'.");
		}
		
	}
	
	private void unRegisterListener(String sensorAbbreviation) {
		
		
		if (sensorAbbreviation.equalsIgnoreCase("accel") && (this.accelSensor != null)) { 
			this.isRegistered_accel = false;
			this.sensorManager.unregisterListener(this, this.accelSensor);
			
		} else if (sensorAbbreviation.equalsIgnoreCase("light") && (this.lightSensor != null)) { 
			this.isRegistered_light = false;
			this.sensorManager.unregisterListener(this, this.lightSensor); 
			
		} else if (sensorAbbreviation.equalsIgnoreCase("network") && (this.telephonyManager != null)) { 
			this.isRegistered_network = false;
			this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_NONE); 
			
		} else {
			Log.e(logTag, "Listener failed to unregister for '"+sensorAbbreviation+"'.");
		}
	}
	
	private void saveSystemStatValuesToDatabase(String statAbbreviation) {
		
		try {

			if (statAbbreviation.equalsIgnoreCase("light")) {
				
				List<long[]> lightSensorValuesCache = this.lightSensorValues;
				this.lightSensorValues = new ArrayList<long[]>();
				
				for (long[] lightVals : lightSensorValuesCache) {
					app.deviceSensorDb.dbLightMeter.insert(new Date(lightVals[0]), lightVals[1], "");
				}
				
				if (lightSensorValuesCache.size() > 0) {
					Log.d(logTag, "Saved "+lightSensorValuesCache.size()+" measurements to '"+statAbbreviation+"' database.");
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

				if (accelValuesCache.size() > 0) {
					Log.d(logTag, "Saved "+accelValuesCache.size()+" measurements to '"+statAbbreviation+"' database.");
				}
					
			} else if (statAbbreviation.equalsIgnoreCase("network")) {
				
				List<String[]> networkValuesCache = this.networkValues;
				this.networkValues = new ArrayList<String[]>();
				
				for (String[] signalVals : networkValuesCache) {
					app.deviceSystemDb.dbNetwork.insert(new Date((long) Long.parseLong(signalVals[0])), (int) Integer.parseInt(signalVals[1]), signalVals[2], signalVals[3]);
				}

				if (networkValuesCache.size() > 0) {
					Log.d(logTag, "Saved "+networkValuesCache.size()+" measurements to '"+statAbbreviation+"' database.");
				}
						 
			} else {
				Log.e(logTag, "Value info for '"+statAbbreviation+"' could not be saved to database.");
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
}
