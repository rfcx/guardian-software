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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import guardian.RfcxGuardian;

public class DevicePositionService extends Service implements LocationListener, SensorEventListener {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DevicePositionService.class);
	
	private static final String SERVICE_NAME = "DevicePosition";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DevicePositionSvc devicePositionSvc;
	
//	private SensorManager sensorManager;
//	private Sensor accelSensor;
	

//	private List<long[]> accelSensorValues = new ArrayList<long[]>();
	
	
	private boolean isListenerRegistered_gps = false;
	private boolean isListenerRegistered_accel = false;

	private boolean allowListenerRegistration_gps = true;
	private boolean allowListenerRegistration_accel = true;

	private LocationManager gpsLocationManager;
	private Location gpsLocation;
	
	private static final String gpsProvider = LocationManager.GPS_PROVIDER;
	private static final long gpsMinimumDistanceChangeBetweenUpdatesInMeters = 10;
	private static final long gpsMinimumTimeElapsedBetweenUpdatesInMilliseconds = 15000;
	
	private static final int ACCEL_FLOAT_MULTIPLIER = 1000000;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.devicePositionSvc = new DevicePositionSvc();
		app = (RfcxGuardian) getApplication();
		
		registerListener("gps");

	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.devicePositionSvc.start();
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
		this.devicePositionSvc.interrupt();
		this.devicePositionSvc = null;
		
		unRegisterListener("gps");
	}
	
	
	private class DevicePositionSvc extends Thread {
		
		public DevicePositionSvc() {
			super("DevicePositionService-DevicePositionSvc");
		}
		
		@Override
		public void run() {
			DevicePositionService devicePositionService = DevicePositionService.this;

			app = (RfcxGuardian) getApplication();
			
			long captureCycleDuration = (long) Math.round( app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") * 0.6667 * 1000 );
					
			while (devicePositionService.runFlag) {
				
				try {
					
					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					Thread.sleep(captureCycleDuration);
					
					updatePosition("gps");
					
					if (gpsLocation != null) {
						Log.i(logTag, "GPS: "+gpsLocation.getLatitude()+", "+gpsLocation.getLongitude());
					} else {
						Log.e(logTag, "GPS: no results yet");
					}
					
				//	registerListener("gps");
//					
//					if (cpuUsageRecordingIncrement < cpuUsageReportingSampleCount) {
//						
//						Thread.sleep(cpuUsageCycleDelayRemainderMilliseconds);
//						
//						if (cpuUsageRecordingIncrement == Math.round(cpuUsageReportingSampleCount/2)) {
//							// quickly toggle accelerometer listener (results to be averaged and saved later in the cycle)
//							registerListener("accel");
//						}
//						
//					} else {
//
//						app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
//
//						// cache pre-captured sensor data
//						saveSystemStatValuesToDatabase("light");
//						saveSystemStatValuesToDatabase("accel");
//						saveSystemStatValuesToDatabase("telephony");
//						
//						// capture and cache data transfer stats
//						dataTransferValues.add(app.deviceNetworkStats.getDataTransferStatsSnapshot());
//						saveSystemStatValuesToDatabase("datatransfer");
//
//						// capture and cache battery level stats
//						batteryLevelValues.add(app.deviceBattery.getBatteryState(app.getApplicationContext(), null));
//						saveSystemStatValuesToDatabase("battery");
//						
//						cpuUsageValues.add(app.deviceCPU.getCurrentStats());
//						saveSystemStatValuesToDatabase("cpu");
//						cpuUsageRecordingIncrement = 0;
//					}
//					
				} catch (InterruptedException e) {
					devicePositionService.runFlag = false;
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					RfcxLog.logExc(logTag, e);
				}
			}
			Log.v(logTag, "Stopping service: "+logTag);
		}		
	}
	
	
	// GPS methods
	
	@Override
    public void onLocationChanged(Location gpsLoc) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    	
    }

    @Override
    public void onProviderEnabled(String provider) {
    	
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }
	
	
	// Sensor methods

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		int eventType = event.sensor.getType();
		
//		if (eventType == Sensor.TYPE_ACCELEROMETER) {
//			this.accelSensorValues.add( new long[] { 
//					(new Date()).getTime(), 
//					(long) Math.round(event.values[0]*ACCEL_FLOAT_MULTIPLIER), 
//					(long) Math.round(event.values[1]*ACCEL_FLOAT_MULTIPLIER), 
//					(long) Math.round(event.values[2]*ACCEL_FLOAT_MULTIPLIER) 
//				} );
//			if (this.isListenerRegistered_accel) { unRegisterListener("accel"); }
//		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}
	
	private void updatePosition(String sensorAbbreviation) {
		
		if (sensorAbbreviation.equalsIgnoreCase("gps") && allowListenerRegistration_gps) {
			
			if (gpsLocationManager != null) {
				gpsLocation = gpsLocationManager.getLastKnownLocation(gpsProvider);
			}
		}
		
	}
	
	private void registerListener(String sensorAbbreviation) {
		
		
		if (sensorAbbreviation.equalsIgnoreCase("gps") && allowListenerRegistration_gps) {
			
			gpsLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			
			if (		gpsLocationManager.isProviderEnabled(gpsProvider)
				&& 	!gpsProvider.isEmpty()
				) {
				
				Log.i(logTag, "GPS: requesting location updates...");
				
				gpsLocationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER,
						gpsMinimumTimeElapsedBetweenUpdatesInMilliseconds,
						gpsMinimumDistanceChangeBetweenUpdatesInMeters,
						this
						);
				
				updatePosition("gps");
				
				isListenerRegistered_gps = true;
				
			} else {
				allowListenerRegistration_gps = false;
				Log.d(logTag, "Disabling Listener Registration for GPS because it doesn't seem to be present.");
			}
			
		} else if (sensorAbbreviation.equalsIgnoreCase("accel") && allowListenerRegistration_accel) {

//			sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			
//			if (sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
//				accelSensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
//				sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
//				isListenerRegistered_accel = true;
//			} else {
				allowListenerRegistration_accel = false;
				Log.d(logTag, "Disabling Listener Registration for Accelerometer because it doesn't seem to be present.");
//			}
			
		} else {
			Log.e(logTag, "Listener failed to register for '"+sensorAbbreviation+"'.");
		}
		
	}
	
	private void unRegisterListener(String sensorAbbreviation) {
		
		if (sensorAbbreviation.equalsIgnoreCase("gps") && (gpsLocation != null)) { 
			isListenerRegistered_gps = false;
			if (gpsLocationManager != null) {
				gpsLocationManager.removeUpdates(this);
			}
			
		} else {
			Log.e(logTag, "Listener failed to unregister for '"+sensorAbbreviation+"'.");
		}
		
	}
//	
//	private void saveSystemStatValuesToDatabase(String statAbbreviation) {
//		
//		try {
//			
//			if (statAbbreviation.equalsIgnoreCase("accel")) {
//				
//				List<long[]> accelValuesCache = accelSensorValues;
//				accelSensorValues = new ArrayList<long[]>();
//				
//				long[] avgAccelVals = new long[] { 0, 0, 0, 0 };
//				int sampleCount = accelValuesCache.size();
//				
//				if (sampleCount > 0) {
//
//					for (long[] accelVals : accelValuesCache) {
//						avgAccelVals[0] = accelVals[0];
//						avgAccelVals[1] = avgAccelVals[1]+accelVals[1];
//						avgAccelVals[2] = avgAccelVals[2]+accelVals[2];
//						avgAccelVals[3] = avgAccelVals[3]+accelVals[3];
//						sampleCount++;
//					}
//					
//					avgAccelVals[1] = (long) Math.round(avgAccelVals[1]/sampleCount);
//					avgAccelVals[2] = (long) Math.round(avgAccelVals[2]/sampleCount);
//					avgAccelVals[3] = (long) Math.round(avgAccelVals[3]/sampleCount);
//		
//					app.deviceSensorDb.dbAccelerometer.insert(
//							new Date(avgAccelVals[0]), 
//							(((double) avgAccelVals[1])/ACCEL_FLOAT_MULTIPLIER)
//							+","+(((double) avgAccelVals[2])/ACCEL_FLOAT_MULTIPLIER)
//							+","+(((double) avgAccelVals[3])/ACCEL_FLOAT_MULTIPLIER),
//							sampleCount);
//				}
//					
//			} else {
//				Log.e(logTag, "Value info for '"+statAbbreviation+"' could not be saved to database.");
//			}
//			
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}
//	}
	
}
