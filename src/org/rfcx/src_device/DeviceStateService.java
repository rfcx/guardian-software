package org.rfcx.src_device;

import org.rfcx.rfcx_src_android.RfcxSource;
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
	
	private static final boolean DEVICE_STATS_ENABLED = true;
	
	static final int DELAY = 1000 - DeviceCpuUsage.SAMPLE_LENGTH;
	private boolean runFlag = false;
	private CpuServiceCheck cpuServiceCheck;

	private static final boolean RECORD_AVERAGE_TO_DATABASE = true;
	private int recordingIncrement = 0;
	
	private SensorManager sensorManager;
	Sensor accelSensor = null;
	Sensor lightSensor = null;
	
	private RfcxSource rfcxSource = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onCreate()"); }
		this.cpuServiceCheck = new CpuServiceCheck();
		registerSensorListeners();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onStart()"); }
		this.runFlag = true;
		rfcxSource = (RfcxSource) getApplication();
		rfcxSource.isServiceRunning_DeviceState = true;
		this.cpuServiceCheck.start();
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onDestroy()"); }
		this.runFlag = false;
		rfcxSource.isServiceRunning_DeviceState = false;
		this.cpuServiceCheck.interrupt();
		this.cpuServiceCheck = null;
		unRegisterSensorListeners();
	}
	
	public static boolean isDeviceStateEnabled() {
		return DEVICE_STATS_ENABLED;
	}
	
	private class CpuServiceCheck extends Thread {
		
		public CpuServiceCheck() {
			super("CpuServiceCheck-CpuService");
		}
		
		@Override
		public void run() {
			DeviceStateService cpuService = DeviceStateService.this;
			rfcxSource = (RfcxSource) getApplication();
			DeviceCpuUsage deviceCpuUsage = rfcxSource.deviceCpuUsage;
			while (cpuService.runFlag) {
				try {
					deviceCpuUsage.updateCpuUsage();
					if (RECORD_AVERAGE_TO_DATABASE) {
						recordingIncrement++;
						if (recordingIncrement == DeviceCpuUsage.AVERAGE_LENGTH) {
							rfcxSource.deviceStateDb.dbCpu.insert(deviceCpuUsage.getCpuUsageAvg());
							recordingIncrement = 0;
							if (RfcxSource.verboseLog()) { Log.d(TAG, "CPU Usage: "+deviceCpuUsage.getCpuUsageAvg()+"%"); }
						}
					}
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					cpuService.runFlag = false;
					rfcxSource.isServiceRunning_DeviceState = true;
				}
			}
		}		
	}

	
	// Sensor methods

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (rfcxSource != null) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				return;
			} else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
				rfcxSource.deviceState.setLightLevel(Math.round(event.values[0]));
				return;
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
		if (this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
			accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
			this.sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		if (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0) {
			lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
			this.sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}
	
	private void unRegisterSensorListeners() {
		if (accelSensor != null) {
			this.sensorManager.unregisterListener(this, accelSensor);
		}
		if (lightSensor != null) {
			this.sensorManager.unregisterListener(this, lightSensor);
		}
	}
}
