package org.rfcx.guardian.admin.device.android.control;

import org.rfcx.guardian.utility.datetime.SntpClient;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import org.rfcx.guardian.admin.RfcxGuardian;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class SntpSyncJobService extends Service {

	private static final String SERVICE_NAME = "SntpSyncJob";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SntpSyncJobService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private SntpSyncJob sntpSyncJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
		
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.sntpSyncJob = new SntpSyncJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.sntpSyncJob.start();
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
		this.sntpSyncJob.interrupt();
		this.sntpSyncJob = null;
	}
	
	
	private class SntpSyncJob extends Thread {
		
		public SntpSyncJob() {
			super("SntpSyncJobService-SntpSyncJob");
		}
		
		@Override
		public void run() {
			SntpSyncJobService sntpSyncJobInstance = SntpSyncJobService.this;
			
			app = (RfcxGuardian) getApplication();
			
			try {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				
				if (!app.deviceConnectivity.isConnected()) {
					
					Log.v(logTag, "No SNTP Sync Job because org.rfcx.guardian.guardian currently has no connectivity.");
					
				} else {

					SntpClient sntpClient = new SntpClient();
					String dateTimeNtpHost = app.rfcxPrefs.getPrefAsString("api_ntp_host");
					
					if (sntpClient.requestTime(dateTimeNtpHost, 15000) && sntpClient.requestTime(dateTimeNtpHost, 15000)) {
						long nowSystem = System.currentTimeMillis();
						long nowSntp = sntpClient.getNtpTime() + SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference();
						SystemClock.setCurrentTimeMillis(nowSntp);
						Log.v(logTag, "SNTP DateTime Sync: SNTP: "+nowSntp+" - System: "+nowSystem+" (System was "+Math.abs(nowSystem-nowSntp)+"ms "+
								((nowSystem >= nowSntp) ? "ahead of" : "behind")
								+" SNTP value. System time now synced to SNTP value.)");
					 }
				}
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				sntpSyncJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
