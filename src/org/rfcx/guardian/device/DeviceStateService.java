package org.rfcx.guardian.device;

import java.util.Calendar;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.database.DeviceStateDb;



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
	
	private boolean runFlag = false;
	private DeviceStateSvc deviceStateSvc;

	private int recordingIncrement = 0;
	
	private SensorManager sensorManager;
//	Sensor accelSensor = null;
	Sensor lightSensor = null;
	
	RfcxGuardian app = null;
	
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
		if (app == null) { app = (RfcxGuardian) getApplication(); }
		if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		((RfcxGuardian) getApplication()).isRunning_DeviceState = true;
		this.deviceStateSvc.start();
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isRunning_DeviceState = false;
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
			if (app == null) { app = (RfcxGuardian) getApplication(); }
			while (deviceStateService.runFlag) {
				CpuUsage deviceCpuUsage = app.deviceCpuUsage;
				DeviceState deviceState = app.deviceState;
				DeviceStateDb deviceStateDb = app.deviceStateDb;
				try {
					deviceCpuUsage.updateCpuUsage();
					recordingIncrement++;
					if (recordingIncrement == CpuUsage.REPORTING_SAMPLE_COUNT) {
						deviceState.setBatteryState(app.getApplicationContext(), null);
						deviceStateDb.dbCpu.insert(deviceCpuUsage.getCpuUsageAvg());
						deviceStateDb.dbCpuClock.insert(deviceCpuUsage.getCpuClockAvg());
						deviceStateDb.dbBattery.insert(deviceState.getBatteryPercent());
						deviceStateDb.dbBatteryTemperature.insert(deviceState.getBatteryTemperature());
						recordingIncrement = 0;
						if (app.verboseLog) Log.d(TAG, "CPU: "+deviceCpuUsage.getCpuUsageAvg()+"% - @"+deviceCpuUsage.getCpuClockAvg()+"MHz - )"+(Calendar.getInstance()).getTime().toLocaleString());
					}
											
					int delayMs = (int) Math.round(60000/deviceState.serviceSamplesPerMinute) - CpuUsage.SAMPLE_LENGTH_MS;
					Thread.sleep(delayMs);
				} catch (InterruptedException e) {
					deviceStateService.runFlag = false;
					app.isRunning_DeviceState = true;
				}
			}
			if (app.verboseLog) Log.d(TAG, "Stopping service: "+TAG);
		}		
	}

	
	// Sensor methods

	@Override
	public void onSensorChanged(SensorEvent event) {
		RfcxGuardian rfcxSource = (RfcxGuardian) getApplication();
		if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
			if (event.values[0] >= 0) {
				rfcxSource.deviceState.setLightLevel(Math.round(event.values[0]));
				rfcxSource.deviceStateDb.dbLight.insert(rfcxSource.deviceState.getLightLevel());
//			} else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//				return;
			}
			return;
		} else {
			return;
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
	}
	
	private void unRegisterSensorListeners() {
//		if (accelSensor != null) {
//			this.sensorManager.unregisterListener(this, accelSensor);
//		}
		if (lightSensor != null) {
			this.sensorManager.unregisterListener(this, lightSensor);
		}
	}
	
	
}
