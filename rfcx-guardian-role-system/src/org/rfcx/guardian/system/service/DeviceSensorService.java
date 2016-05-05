package org.rfcx.guardian.system.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.system.RfcxGuardian;
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
import android.util.Log;

public class DeviceSensorService extends Service implements SensorEventListener {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceSensorService.class.getSimpleName();
	
	RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceSensorSvc deviceSensorSvc;
	
	private SensorManager sensorManager;
	private Sensor lightSensor;
	private Sensor accelSensor;
	
	private SignalStrengthListener signalStrengthListener;
	private TelephonyManager telephonyManager;

	private List<long[]> lightSensorValues = new ArrayList<long[]>();
	private List<long[]> accelSensorValues = new ArrayList<long[]>();
	private List<String[]> networkValues = new ArrayList<String[]>();
	
	private static final int ACCEL_FLOAT_MULTIPLIER = 1000;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceSensorSvc = new DeviceSensorSvc();
		
		if (app == null) { app = (RfcxGuardian) getApplication(); }
		
		registerListener("light");
		registerListener("network");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (app == null) { app = (RfcxGuardian) getApplication(); }
		Log.v(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		((RfcxGuardian) getApplication()).isRunning_DeviceSensor = true;
		this.deviceSensorSvc.start();
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isRunning_DeviceSensor = false;
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
			
			if (app == null) { app = (RfcxGuardian) getApplication(); }
			
			long sensorCaptureCycleDuration = (long) Math.round(app.rfcxPrefs.getPrefAsInt("audio_cycle_duration")/3);
			Log.d(TAG, "Pause: "+sensorCaptureCycleDuration);
					
			while (deviceSensorService.runFlag) {
				
				try {
					
					Thread.sleep(sensorCaptureCycleDuration);
					
					saveSensorValuesToDatabase("light");
					saveSensorValuesToDatabase("accel");
					saveSensorValuesToDatabase("network");
					
				} catch (InterruptedException e) {
					deviceSensorService.runFlag = false;
					app.isRunning_DeviceSensor = true;
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
		} else if (sensorAbbreviation.equalsIgnoreCase("light") && (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0)) {
			this.lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
			this.sensorManager.registerListener(this, this.lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
		} else if (sensorAbbreviation.equalsIgnoreCase("network")) {
			this.signalStrengthListener = new SignalStrengthListener();
			this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		} else {
			Log.e(TAG, "Listener failed to unregister for sensor abbreviation '"+sensorAbbreviation+"'.");
		}
		
	}
	
	private void unRegisterListener(String sensorAbbreviation) {
		
		if (sensorAbbreviation.equalsIgnoreCase("accel") && (this.accelSensor != null)) { 
			this.sensorManager.unregisterListener(this, this.accelSensor); 
		} else if (sensorAbbreviation.equalsIgnoreCase("light") && (this.lightSensor != null)) { 
			this.sensorManager.unregisterListener(this, this.lightSensor); 
		} else if (sensorAbbreviation.equalsIgnoreCase("network") && (this.telephonyManager != null)) { 
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
		
		if (sensorAbbreviation.equalsIgnoreCase("light")) {
			
			for (long[] lightVals : this.lightSensorValues) {
				app.deviceStateDb.dbLightMeter.insert(new Date(lightVals[0]), lightVals[1], "");
			}
			this.lightSensorValues = new ArrayList<long[]>();	
			
		} else if (sensorAbbreviation.equalsIgnoreCase("accel")) {
			
			for (long[] accelVals : this.accelSensorValues) {
//				app.deviceStateDb.db.insert(new Date(lightVals[0]), lightVals[1], "");
			}
			this.accelSensorValues = new ArrayList<long[]>();
			
		} else if (sensorAbbreviation.equalsIgnoreCase("network")) {
			
			for (String[] signalVals : this.networkValues) {
				app.deviceStateDb.dbNetwork.insert(new Date((long) Long.parseLong(signalVals[0])), (int) Integer.parseInt(signalVals[1]), signalVals[2], signalVals[3]);
			}
			this.networkValues = new ArrayList<String[]>();	
			
		} else {
			Log.e(TAG, "Sensor value info could not be saved to database for sensor abbreviation '"+sensorAbbreviation+"'.");
		}
		
	}
	
}
