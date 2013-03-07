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

public class DeviceStatsService extends Service implements SensorEventListener {

	private static final String TAG = DeviceStatsService.class.getSimpleName();
	
	private static final boolean DEVICE_STATS_ENABLED = true;
	
	static final int DELAY = 500;
	private boolean runFlag = false;
	private CpuServiceCheck cpuServiceCheck;

	private static final boolean RECORD_AVERAGE_TO_DATABASE = true;
	private int recordingIncrement = 0;
	
	private SensorManager sensorManager;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onCreate()"); }
		this.cpuServiceCheck = new CpuServiceCheck();
		this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		if (this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
			Sensor accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
			this.sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		if (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0) {
			Sensor lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
			this.sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onStart()"); }
		this.runFlag = true;
		this.cpuServiceCheck.start();
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (RfcxSource.verboseLog()) { Log.d(TAG, "onDestroy()"); }
		this.runFlag = false;
		this.cpuServiceCheck.interrupt();
		this.cpuServiceCheck = null;
		this.sensorManager.unregisterListener(this);
	}
	
	public static boolean areDeviceStatsEnabled() {
		return DEVICE_STATS_ENABLED;
	}
	
	private class CpuServiceCheck extends Thread {
		
		public CpuServiceCheck() {
			super("CpuServiceCheck-CpuService");
		}
		
		@Override
		public void run() {
			DeviceStatsService cpuService = DeviceStatsService.this;
			RfcxSource rfcxSource = (RfcxSource) getApplication();
			DeviceCpuUsage deviceCpuUsage = rfcxSource.deviceCpuUsage;
			while (cpuService.runFlag) {
				try {
					deviceCpuUsage.updateCpuUsage();
					if (RECORD_AVERAGE_TO_DATABASE) {
						recordingIncrement++;
						if (recordingIncrement == DeviceCpuUsage.AVERAGE_LENGTH) {
							rfcxSource.deviceStateDb.dbCpu.insert(deviceCpuUsage.getCpuUsageAvg());
							recordingIncrement = 0;
							Log.d(TAG, "CPU: "+deviceCpuUsage.getCpuUsageAvg());
						}
					}
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					cpuService.runFlag = false;
				}
			}
		}		
	}

	
	// Sensor methods

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			return;
		} else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
			Log.d(TAG, "Light: "+event.values[0]);
			return;
		} else {
			return;
		}
	}
	
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}
}
