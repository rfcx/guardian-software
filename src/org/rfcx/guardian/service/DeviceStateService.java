package org.rfcx.guardian.service;

import java.util.Calendar;
import java.util.Date;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.database.DataTransferDb;
import org.rfcx.guardian.database.DeviceStateDb;
import org.rfcx.guardian.database.HardwareDb;
import org.rfcx.guardian.device.DeviceCpuUsage;
import org.rfcx.guardian.device.DeviceState;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.TrafficStats;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

public class DeviceStateService extends Service implements SensorEventListener {

	private static final String TAG = "RfcxGuardian-"+DeviceStateService.class.getSimpleName();
	
	RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceStateSvc deviceStateSvc;

	private int recordingIncrement = 0;
	
	private SensorManager sensorManager;
//	Sensor accelSensor = null;
	Sensor lightSensor = null;
	
	SignalStrengthListener signalStrengthListener;
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceStateSvc = new DeviceStateSvc();
		registerListeners();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (app == null) { app = (RfcxGuardian) getApplication(); }
		Log.v(TAG, "Starting service: "+TAG);
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
		unRegisterListeners();
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
				DeviceCpuUsage deviceCpuUsage = app.deviceCpuUsage;
				DeviceState deviceState = app.deviceState;
				HardwareDb hardwareDb = app.hardwareDb;
				DataTransferDb dataTransferDb = app.dataTransferDb;
				try {
					deviceCpuUsage.updateCpuUsage();
					recordingIncrement++;
					if (recordingIncrement == DeviceCpuUsage.REPORTING_SAMPLE_COUNT) {
						
						deviceState.setBatteryState(app.getApplicationContext(), null);
						
						hardwareDb.dbCPU.insert(new Date(), deviceCpuUsage.getCpuUsageAvg(), deviceCpuUsage.getCpuClockAvg());
						hardwareDb.dbBattery.insert(new Date(), deviceState.getBatteryPercent(), deviceState.getBatteryTemperature());
						hardwareDb.dbPower.insert(new Date(), !deviceState.isBatteryDisCharging(), deviceState.isBatteryCharged());
						
						long[] trafficStats = deviceState.updateTrafficStats();
						dataTransferDb.dbTransferred.insert(new Date(), new Date(trafficStats[0]), new Date(trafficStats[1]), trafficStats[2], trafficStats[3], trafficStats[4], trafficStats[5]);
						Log.i(TAG, "TrafficStats: Received ("+trafficStats[2]/1024+"kB/"+trafficStats[4]/1024+"kB) | Sent ("+trafficStats[3]/1024+"kB/"+trafficStats[5]/1024+"kB)");
						
						recordingIncrement = 0;
						Log.i(TAG, "CPU: "+deviceCpuUsage.getCpuUsageAvg()+"% @"+deviceCpuUsage.getCpuClockAvg()+"MHz "+(Calendar.getInstance()).getTime().toGMTString());
					}
											
					int delayMs = (int) Math.round(60000/deviceState.serviceSamplesPerMinute) - DeviceCpuUsage.SAMPLE_LENGTH_MS;
					Thread.sleep(delayMs);
				} catch (InterruptedException e) {
					deviceStateService.runFlag = false;
					app.isRunning_DeviceState = true;
				}
			}
			Log.v(TAG, "Stopping service: "+TAG);
		}		
	}

	
	// Sensor methods

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (this.app == null) this.app = (RfcxGuardian) getApplication();
		if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
			if (event.values[0] >= 0) {
				this.app.deviceState.setLightLevel(Math.round(event.values[0]));
				this.app.deviceStateDb.dbLight.insert(this.app.deviceState.getLightLevel());
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
	
	private void registerListeners() {
		
		this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//		if (this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
//			accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
//			this.sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
//		}
		if (this.sensorManager.getSensorList(Sensor.TYPE_LIGHT).size() != 0) {
			lightSensor = sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
			this.sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		
		signalStrengthListener = new SignalStrengthListener();
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//		telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
//		telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTH|PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		
		
	}
	
	private void unRegisterListeners() {
//		if (accelSensor != null) {
//			this.sensorManager.unregisterListener(this, accelSensor);
//		}
		if (lightSensor != null) {
			this.sensorManager.unregisterListener(this, lightSensor);
		}
		
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_NONE);	// LISTEN_NONE : Stop listening for updates.
	}
	
	

	
	public class SignalStrengthListener extends PhoneStateListener {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);

			boolean	isGsmActive = signalStrength.isGsm();
	//		int	cdmaRssi = signalStrength.getCdmaDbm(); // CDMA RSSI value in dBm
	//		int	cdmaEcIo = signalStrength.getCdmaEcio(); //CDMA Ec/Io value in dB*10
	//		int	evdoRssi = signalStrength.getEvdoDbm(); //EVDO RSSI value in dBm
	//		int	evdoEcIo = signalStrength.getEvdoEcio(); //EVDO Ec/Io value in dB*10
	//		int	evdoSnr = signalStrength.getEvdoSnr(); //signal to noise ratio. Valid values are 0-8. 8 is the highest.
	//		int	gsmBitErrorRate = signalStrength.getGsmBitErrorRate(); // GSM bit error rate (0-7, 99) as defined in TS 27.007 8.5
			int	gsmSignalStrength = signalStrength.getGsmSignalStrength(); //GSM Signal Strength, valid values are (0-31, 99) as defined in TS 27.007 8.5
			
			int dBmGsmSignalStrength = (-113+2*gsmSignalStrength);
			if (dBmGsmSignalStrength > 0) {
				dBmGsmSignalStrength = 0;
				Log.w(TAG,"No GSM signal found."); 
			} else { 
				Log.w(TAG,dBmGsmSignalStrength+"dBm"); 
			}
			app.deviceStateDb.dbNetworkStrength.insert(dBmGsmSignalStrength);
			
		}
	}
	
	
	
}
