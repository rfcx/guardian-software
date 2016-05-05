package org.rfcx.guardian.system.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.system.device.DeviceCpuUsage;
import org.rfcx.guardian.utility.RfcxConstants;

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

	private static final int RECORDING_CYCLE_DURATION_MS = 20000;
	
	private SensorManager sensorManager;
	Sensor accelSensor = null;
	Sensor lightSensor = null;
	
	SignalStrengthListener signalStrengthListener = null;
	TelephonyManager telephonyManager = null;
	
	List<long[]> accelSensorValues = new ArrayList<long[]>();
	List<long[]> lightSensorValues = new ArrayList<long[]>();
	List<String[]> signalStrengthValues = new ArrayList<String[]>();
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceSensorSvc = new DeviceSensorSvc();
		registerListeners();
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
		unRegisterListeners();
	}
	
	
	private class DeviceSensorSvc extends Thread {
		
		public DeviceSensorSvc() {
			super("DeviceSensorService-DeviceSensorSvc");
		}
		
		@Override
		public void run() {
			DeviceSensorService deviceSensorService = DeviceSensorService.this;
			
			if (app == null) { app = (RfcxGuardian) getApplication(); }
					
			while (deviceSensorService.runFlag) {
				try {
					
					Thread.sleep(RECORDING_CYCLE_DURATION_MS);
					
					for (long[] lightVals : lightSensorValues) {
						app.deviceStateDb.dbLightMeter.insert(new Date(lightVals[0]), lightVals[1], "");
					}
					lightSensorValues = new ArrayList<long[]>();
					
					for (long[] accelVals : accelSensorValues) {
			//			app.deviceStateDb.db.insert(new Date(lightVals[0]), lightVals[1], "");
					}
					accelSensorValues = new ArrayList<long[]>();
					
					for (String[] signalVals : signalStrengthValues) {
						app.deviceStateDb.dbNetwork.insert(new Date((long) Long.parseLong(signalVals[0])), (int) Integer.parseInt(signalVals[1]), signalVals[2], signalVals[3]);
					}
					signalStrengthValues = new ArrayList<String[]>();					
					
					
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
		if (this.app == null) this.app = (RfcxGuardian) getApplication();
		if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
			if (event.values[0] >= 0) {
				this.lightSensorValues.add(new long[] { (new Date()).getTime(), (long) Math.round(event.values[0]) });
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
		
		this.signalStrengthListener = new SignalStrengthListener();
		this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		
	}
	
	private void unRegisterListeners() {
		
		if (this.accelSensor != null) { this.sensorManager.unregisterListener(this, this.accelSensor); }
		
		if (this.lightSensor != null) { this.sensorManager.unregisterListener(this, this.lightSensor); }
		
		if (this.telephonyManager != null) { this.telephonyManager.listen(this.signalStrengthListener, PhoneStateListener.LISTEN_NONE); }

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
			String networkType = "";
			String carrierName = "";
			
			if (dBmGsmSignalStrength > 0) {
				dBmGsmSignalStrength = 0;
			} else { 
				carrierName = telephonyManager.getNetworkOperatorName();
				networkType = getNetworkTypeCategory(telephonyManager.getNetworkType());
			}
			
			signalStrengthValues.add(new String[] { ""+(new Date()).getTime(), ""+dBmGsmSignalStrength, networkType, carrierName } );
			
		}
	}
	
	private static String getNetworkTypeCategory(int getNetworkType) {
		String networkTypeCategory = null;
	    switch (getNetworkType) {
	        case TelephonyManager.NETWORK_TYPE_UNKNOWN:
	        	networkTypeCategory = "unknown";
	            break;
	        case TelephonyManager.NETWORK_TYPE_IDEN:
	        	networkTypeCategory = "iden";
	            break;
	        case TelephonyManager.NETWORK_TYPE_GPRS:
	        	networkTypeCategory = "gprs";
	            break;
	        case TelephonyManager.NETWORK_TYPE_EDGE:
	        	networkTypeCategory = "edge";
	            break;
	        case TelephonyManager.NETWORK_TYPE_UMTS:
	        	networkTypeCategory = "umts";
	            break;
	        case TelephonyManager.NETWORK_TYPE_CDMA:
	        	networkTypeCategory = "cdma";
	            break;
	        case TelephonyManager.NETWORK_TYPE_1xRTT:
	        	networkTypeCategory = "1xrtt";
	            break;
	        case TelephonyManager.NETWORK_TYPE_EVDO_0:
	        	networkTypeCategory = "evdo0";
	            break;
	        case TelephonyManager.NETWORK_TYPE_EVDO_A:
	        	networkTypeCategory = "evdoA";
	            break;
	        case TelephonyManager.NETWORK_TYPE_EVDO_B:
	        	networkTypeCategory = "evdoB";
	            break;
	        case TelephonyManager.NETWORK_TYPE_HSDPA:
	        	networkTypeCategory = "hsdpa";
	            break;
	        case TelephonyManager.NETWORK_TYPE_HSUPA:
	        	networkTypeCategory = "hsupa";
	            break;
	        case TelephonyManager.NETWORK_TYPE_HSPA:
	        	networkTypeCategory = "hspa";
	            break;
	        default:
	        	networkTypeCategory = null;
	    }
	    return networkTypeCategory;
	}
	
}
