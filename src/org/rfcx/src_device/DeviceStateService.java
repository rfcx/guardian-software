package org.rfcx.src_device;

import org.rfcx.src_android.RfcxSource;
import org.rfcx.src_database.DeviceStateDb;
import org.rfcx.src_util.DeviceCpuUsage;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class DeviceStateService extends Service implements SensorEventListener {

	private static final String TAG = DeviceStateService.class.getSimpleName();
	
	private static final int DELAY = 1000 - DeviceCpuUsage.SAMPLE_LENGTH;
	private boolean runFlag = false;
	private DeviceStateSvc deviceStateSvc;

	private int recordingIncrement = 0;
	
	private SensorManager sensorManager;
//	Sensor accelSensor = null;
	Sensor lightSensor = null;
//	Sensor tempSensor = null;
	
	private RfcxSource rfcxSource = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceStateSvc = new DeviceStateSvc();
		registerSensorListeners();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (RfcxSource.VERBOSE) Log.d(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		if (rfcxSource == null) rfcxSource = (RfcxSource) getApplication();
		rfcxSource.isServiceRunning_DeviceState = true;
		this.deviceStateSvc.start();
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		rfcxSource.isServiceRunning_DeviceState = false;
		this.deviceStateSvc.interrupt();
		this.deviceStateSvc = null;
		unRegisterSensorListeners();
	}
	
	
	private class DeviceStateSvc extends Thread {
		
		public DeviceStateSvc() {
			super("DeviceStateService-DeviceStateSvc");
		}
		
		@Override
		public void run() {
			DeviceStateService deviceStateService = DeviceStateService.this;
			if (rfcxSource == null) rfcxSource = (RfcxSource) getApplication();
			DeviceCpuUsage deviceCpuUsage = rfcxSource.deviceCpuUsage;
			DeviceState deviceState = rfcxSource.deviceState;
			DeviceStateDb deviceStateDb = rfcxSource.deviceStateDb;
			while (deviceStateService.runFlag) {
				try {
					deviceCpuUsage.updateCpuUsage();
					recordingIncrement++;
					if (recordingIncrement == DeviceCpuUsage.REPORTING_SAMPLE_COUNT) {
//						deviceState.setBatteryState(rfcxSource.getApplicationContext(), serviceIntent);
						deviceStateDb.dbCpu.insert(deviceCpuUsage.getCpuUsageAvg());
						deviceStateDb.dbCpuClock.insert(deviceCpuUsage.getCpuClockAvg());
						deviceStateDb.dbBattery.insert(deviceState.getBatteryPercent());
						recordingIncrement = 0;
						if (RfcxSource.VERBOSE) Log.d(TAG, "CPU: "+deviceCpuUsage.getCpuUsageAvg()+"% - @"+deviceCpuUsage.getCpuClockAvg()+"Hz)");
					}
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					deviceStateService.runFlag = false;
					rfcxSource.isServiceRunning_DeviceState = true;
				}
			}
			if (RfcxSource.VERBOSE) Log.d(TAG, "Stopping service: "+TAG);
		}		
	}

	
	// Sensor methods

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (rfcxSource != null) {
			if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
				if (event.values[0] >= 0) {
					rfcxSource.deviceState.setLightLevel(Math.round(event.values[0]));
					rfcxSource.deviceStateDb.dbLight.insert(rfcxSource.deviceState.getLightLevel());
				}
				return;
//			} else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//				return;
//			} else if (event.sensor.getType() == Sensor.TYPE_TEMPERATURE) {
//				Log.d(TAG, "Temperature: "+event.values[0]);
//				return;
			} else {
				return;
			}
		}
	}
	
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}
	
	private void registerSensorListeners() {
		this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//		if (this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
//			accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
//			this.sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
//		}
		if (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0) {
			lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
			this.sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
//		if (this.sensorManager.getSensorList(Sensor.TYPE_TEMPERATURE).size() != 0) {
//			tempSensor = sensorManager.getSensorList(Sensor.TYPE_TEMPERATURE).get(0);
//			this.sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
//		} else {
//			Log.d(TAG, "No Temperature sensor");
//		}
	}
	
	private void unRegisterSensorListeners() {
//		if (accelSensor != null) {
//			this.sensorManager.unregisterListener(this, accelSensor);
//		}
		if (lightSensor != null) {
			this.sensorManager.unregisterListener(this, lightSensor);
		}
//		if (tempSensor != null) {
//			this.sensorManager.unregisterListener(this, tempSensor);
//		}
	}
	
	
}
