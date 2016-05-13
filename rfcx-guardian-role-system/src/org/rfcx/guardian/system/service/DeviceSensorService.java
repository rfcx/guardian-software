package org.rfcx.guardian.system.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.device.DeviceMobileNetwork;

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
import android.text.TextUtils;
import android.util.Log;

public class DeviceSensorService extends Service implements SensorEventListener {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceSensorService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "DeviceSensor";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceSensorSvc deviceSensorSvc;
	
	private SensorManager sensorManager;
	private Sensor lightSensor;
	private Sensor accelSensor;
	
	private SignalStrengthListener signalStrengthListener;
	private TelephonyManager telephonyManager;

	// local data lists for queuing captured sensor data and writing to disk in batches
	private List<long[]> lightSensorValues = new ArrayList<long[]>();
	private List<long[]> accelSensorValues = new ArrayList<long[]>();
	private List<String[]> networkValues = new ArrayList<String[]>();
	
	private boolean isRegistered_light = false;
	private boolean isRegistered_accel = false;
	private boolean isRegistered_network = false;
	
	private static final int ACCEL_FLOAT_MULTIPLIER = 1000000;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceSensorSvc = new DeviceSensorSvc();
		app = (RfcxGuardian) getApplication();
		
		registerListener("light");
		registerListener("network");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceSensorSvc.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(TAG, e);
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.deviceSensorSvc.interrupt();
		this.deviceSensorSvc = null;
		
		unRegisterListener("light");
		unRegisterListener("network");
	}
	
	
	private class DeviceSensorSvc extends Thread {
		
		public DeviceSensorSvc() {
			super("DeviceSensorService-DeviceSensorSvc");
		}
		
		@Override
		public void run() {
			DeviceSensorService deviceSensorService = DeviceSensorService.this;

			app = (RfcxGuardian) getApplication();
			
		//	long sensorCaptureCycleDurationHalf = (long) Math.round(app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") / 4 );
			long sensorCaptureCycleDurationHalf = 22000;
					
			while (deviceSensorService.runFlag) {
				
				try {

					Thread.sleep(sensorCaptureCycleDurationHalf);
					
					// quickly toggle accelerometer listener (results to be averaged and saved later in the cycle)
					registerListener("accel");
					
					Thread.sleep(sensorCaptureCycleDurationHalf);
					
					saveSensorValuesToDatabase("light");
					saveSensorValuesToDatabase("network");
					saveSensorValuesToDatabase("accel");
					
				} catch (InterruptedException e) {
					deviceSensorService.runFlag = false;
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					RfcxLog.logExc(TAG, e);
				}
			}
			Log.v(TAG, "Stopping service: "+TAG);
		}		
	}

	
	// Sensor methods

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		int eventType = event.sensor.getType();
		
		if (eventType == Sensor.TYPE_LIGHT) {
			this.lightSensorValues.add( new long[] { 
					(new Date()).getTime(), 
					(long) Math.round(event.values[0]) 
				} );
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
			Log.e(TAG, "Listener failed to unregister for sensor abbreviation '"+sensorAbbreviation+"'.");
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
			Log.e(TAG, "Listener failed to unregister for sensor abbreviation '"+sensorAbbreviation+"'.");
		}
	}
	
	public class SignalStrengthListener extends PhoneStateListener {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);
			networkValues.add(DeviceMobileNetwork.getMobileNetworkSummary(telephonyManager, signalStrength));
		}
	}
	
	private void saveSensorValuesToDatabase(String sensorAbbreviation) {
		
		try {

			if (sensorAbbreviation.equalsIgnoreCase("light")) {
				for (long[] lightVals : this.lightSensorValues) {
					app.deviceStateDb.dbLightMeter.insert(new Date(lightVals[0]), lightVals[1], "");
				}
				this.lightSensorValues = new ArrayList<long[]>();	
				
			} else if (sensorAbbreviation.equalsIgnoreCase("network")) {
				
				for (String[] signalVals : this.networkValues) {
					app.deviceStateDb.dbNetwork.insert(new Date((long) Long.parseLong(signalVals[0])), (int) Integer.parseInt(signalVals[1]), signalVals[2], signalVals[3]);
				}
				this.networkValues = new ArrayList<String[]>();	
				
			} else if (sensorAbbreviation.equalsIgnoreCase("accel")) {
				
					List<long[]> accelValsSnapshot = this.accelSensorValues;
					this.accelSensorValues = new ArrayList<long[]>();
					long[] avgAccelVals = new long[] { 0, 0, 0, 0 };
					int sampleCount = accelValsSnapshot.size();
					
					if (sampleCount > 0) {

						for (long[] accelVals : accelValsSnapshot) {
							avgAccelVals[0] = accelVals[0];
							avgAccelVals[1] = avgAccelVals[1]+accelVals[1];
							avgAccelVals[2] = avgAccelVals[2]+accelVals[2];
							avgAccelVals[3] = avgAccelVals[3]+accelVals[3];
							sampleCount++;
						}
						
						avgAccelVals[1] = (long) Math.round(avgAccelVals[1]/sampleCount);
						avgAccelVals[2] = (long) Math.round(avgAccelVals[2]/sampleCount);
						avgAccelVals[3] = (long) Math.round(avgAccelVals[3]/sampleCount);
			
						app.deviceStateDb.dbAccelerometer.insert(
								new Date(avgAccelVals[0]), 
								(((double) avgAccelVals[1])/ACCEL_FLOAT_MULTIPLIER)
								+","+(((double) avgAccelVals[2])/ACCEL_FLOAT_MULTIPLIER)
								+","+(((double) avgAccelVals[3])/ACCEL_FLOAT_MULTIPLIER),
								sampleCount);
					}
	
			} else {
				Log.e(TAG, "Sensor value info could not be saved to database for sensor abbreviation '"+sensorAbbreviation+"'.");
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(TAG, e);
		}
	}
	
}
