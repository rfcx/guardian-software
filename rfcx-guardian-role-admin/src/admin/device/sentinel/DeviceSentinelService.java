package admin.device.sentinel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import admin.RfcxGuardian;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceSentinelService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceSentinelService.class);
	
	private static final String SERVICE_NAME = "DeviceSentinel";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceSentinelSvc deviceSentinelSvc;
	
	private static final long SENTINEL_POWER_MEASUREMENT_LOOP_MS = 20000;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceSentinelSvc = new DeviceSentinelSvc();
		app = (RfcxGuardian) getApplication();
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceSentinelSvc.start();
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
		this.deviceSentinelSvc.interrupt();
		this.deviceSentinelSvc = null;
	}
	
	
	private class DeviceSentinelSvc extends Thread {
		
		public DeviceSentinelSvc() {
			super("DeviceSentinelService-DeviceSentinelSvc");
		}
		
		@Override
		public void run() {
			DeviceSentinelService deviceSentinelService = DeviceSentinelService.this;

			app = (RfcxGuardian) getApplication();
						
			SentinelPowerUtils sentinelPowerUtils = new SentinelPowerUtils(app.getApplicationContext());
			
			while (deviceSentinelService.runFlag) {
				
				try {
					
					Thread.sleep(SENTINEL_POWER_MEASUREMENT_LOOP_MS);

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
					// Update Sentinel Power Stats
					sentinelPowerUtils.updateSentinelPowerValues();
					
					String[] batteryValues = sentinelPowerUtils.getCurrentValues("battery");
					app.sentinelPowerDb.dbExternalPowerBattery.insert(new Date(), batteryValues[0], batteryValues[1], batteryValues[2], "");
					
					String[] inputValues = sentinelPowerUtils.getCurrentValues("input");
					app.sentinelPowerDb.dbExternalPowerInput.insert(new Date(), inputValues[0], inputValues[1], inputValues[2], "");
					
					String[] loadValues = sentinelPowerUtils.getCurrentValues("load");
					app.sentinelPowerDb.dbExternalPowerLoad.insert(new Date(), loadValues[0], loadValues[1], loadValues[2], "");
					
				} catch (InterruptedException e) {
					deviceSentinelService.runFlag = false;
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					RfcxLog.logExc(logTag, e);
				}
			}
			Log.v(logTag, "Stopping service: "+logTag);
		}		
	}
	
//	
//	
//	private void saveSystemStatValuesToDatabase(String statAbbreviation) {
//		
//		try {
//			
//			if (statAbbreviation.equalsIgnoreCase("cpu")) {
//				
//				List<int[]> cpuUsageValuesCache = this.cpuUsageValues;
//				this.cpuUsageValues = new ArrayList<int[]>();
//				
//				for (int[] cpuVals : cpuUsageValuesCache) {
//					app.deviceSystemDb.dbCPU.insert(new Date(), cpuVals[0], cpuVals[1]);
//				}
//				
//			} else if (statAbbreviation.equalsIgnoreCase("battery")) {
//				
//				List<int[]> batteryLevelValuesCache = this.batteryLevelValues;
//				this.batteryLevelValues = new ArrayList<int[]>();
//				
//				for (int[] batteryLevelVals : batteryLevelValuesCache) {
//					app.deviceSystemDb.dbBattery.insert(new Date(), batteryLevelVals[0], batteryLevelVals[1]);
//					app.deviceSystemDb.dbPower.insert(new Date(), batteryLevelVals[2], batteryLevelVals[3]);
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
