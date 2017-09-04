package org.rfcx.guardian.system.service;

import java.util.Date;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.system.device.DeviceCpuUsage;
import org.rfcx.guardian.utility.device.DeviceCPU;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DeviceStateService extends Service {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceStateService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "DeviceState";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceStateSvc deviceStateSvc;

	private int recordingIncrement = 0;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceStateSvc = new DeviceStateSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceStateSvc.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(TAG, e);
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.deviceStateSvc.interrupt();
		this.deviceStateSvc = null;
	}
	
	
	private class DeviceStateSvc extends Thread {
		
		public DeviceStateSvc() {
			super("DeviceStateService-DeviceStateSvc");
		}
		
		@Override
		public void run() {
			DeviceStateService deviceStateService = DeviceStateService.this;
			
			app = (RfcxGuardian) getApplication();

			long REPORTING_LOOP_MS = 45000;
			long MEASUREMENT_LOOP_MS = 1000;
			
			int cpuSampleCountPerReportingLoop = Math.round( REPORTING_LOOP_MS / MEASUREMENT_LOOP_MS );
			
			long CYCLE_DELAY_MS = MEASUREMENT_LOOP_MS - DeviceCPU.MEASUREMENT_DURATION_MS;
			
			while (deviceStateService.runFlag) {
				
				try {
					
					app.deviceCPU.takeMeasurement();
					
					recordingIncrement++;
					
					if (recordingIncrement < cpuSampleCountPerReportingLoop) {
						
						Thread.sleep(CYCLE_DELAY_MS);
						
					} else {
						
						app.deviceStateDb.dbCPU.insert(new Date(), app.deviceCpuUsage.getCpuUsageAvg(), app.deviceCpuUsage.getCpuClockAvg());
						
						int[] batteryStats = app.deviceBattery.getBatteryState(app.getApplicationContext(), null);
						app.deviceStateDb.dbBattery.insert(new Date(), batteryStats[0], batteryStats[1]);
						app.deviceStateDb.dbPower.insert(new Date(), batteryStats[2], batteryStats[3]);
						
						long[] networkStats = app.deviceNetworkStats.getDataTransferStatsSnapshot();
						// before saving, make sure this isn't the first time the stats are being generated (that throws off the net change figures)
						if (networkStats[6] == 0) {
							app.dataTransferDb.dbTransferred.insert(new Date(), new Date(networkStats[0]), new Date(networkStats[1]), networkStats[2], networkStats[3], networkStats[4], networkStats[5]);
						}
						
						recordingIncrement = 0;
					}
					
				} catch (InterruptedException e) {
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					deviceStateService.runFlag = false;
					RfcxLog.logExc(TAG, e);
				}
			}
			Log.v(TAG, "Stopping service: "+TAG);
		}		
	}

}
