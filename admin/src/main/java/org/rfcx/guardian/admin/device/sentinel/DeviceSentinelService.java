package org.rfcx.guardian.admin.device.sentinel;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceSentinelService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceSentinelService.class);
	
	private static final String SERVICE_NAME = "DeviceSentinel";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceSentinelSvc deviceSentinelSvc;

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
		return START_NOT_STICKY;
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

			try {

				int audioCycleDuration = app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
				long sentinelPowerMeasurementLoopDuration = (long) Math.round( 2 * ( (audioCycleDuration * 1000) / 3 ));

				Log.d(logTag, (new StringBuilder())
						.append("SentinelPowerStats Capture").append(": ")
						.append("Snapshots (all metrics) taken every ").append(Math.round(sentinelPowerMeasurementLoopDuration/1000)).append(" seconds.")
						.toString());
						
				while (deviceSentinelService.runFlag) {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
//					if (app.sentinelPowerUtils.confirmConnection()) {

						app.sentinelPowerUtils.updateSentinelPowerValues();
						app.sentinelPowerUtils.saveSentinelPowerValuesToDatabase(app.getApplicationContext(), false);
						
//					}
					
					Thread.sleep(sentinelPowerMeasurementLoopDuration);
				}
			
			} catch (InterruptedException e) {
				deviceSentinelService.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				RfcxLog.logExc(logTag, e);
			}
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
