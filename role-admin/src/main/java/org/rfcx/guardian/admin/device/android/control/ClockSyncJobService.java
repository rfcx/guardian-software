package org.rfcx.guardian.admin.device.android.control;

import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.network.SntpUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class ClockSyncJobService extends Service {

	private static final String SERVICE_NAME = "ClockSyncJob";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ClockSyncJobService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ClockSyncJob clockSyncJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
		
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.clockSyncJob = new ClockSyncJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.clockSyncJob.start();
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
		this.clockSyncJob.interrupt();
		this.clockSyncJob = null;
	}
	
	
	private class ClockSyncJob extends Thread {
		
		public ClockSyncJob() {
			super("ClockSyncJobService-ClockSyncJob");
		}
		
		@Override
		public void run() {
			ClockSyncJobService clockSyncJobInstance = ClockSyncJobService.this;
			
			app = (RfcxGuardian) getApplication();
			
			try {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				long[] sntpClockValues = SntpUtils.getSntpClockValues(app.deviceConnectivity.isConnected(), app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_NTP_HOST));

				if (sntpClockValues.length > 0) {
					long nowSntp = sntpClockValues[0];
					long nowSystem = sntpClockValues[1];
					app.deviceSystemDb.dbDateTimeOffsets.insert(nowSystem, "sntp", (nowSntp-nowSystem), DateTimeUtils.getTimeZoneOffset());
					SystemClock.setCurrentTimeMillis(nowSntp);
					Log.v(logTag, "DateTime Sync: System time now synced to SNTP value.");
				}
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				clockSyncJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME, false);
			}
		}
	}

	
}
