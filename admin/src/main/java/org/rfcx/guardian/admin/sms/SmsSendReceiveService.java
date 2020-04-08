package org.rfcx.guardian.admin.sms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SmsSendReceiveService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, SmsSendReceiveService.class);
	
	private static final String SERVICE_NAME = "SmsSendReceive";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private SmsSendReceiveJob smsSendReceiveJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.smsSendReceiveJob = new SmsSendReceiveJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.smsSendReceiveJob.start();
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
		this.smsSendReceiveJob.interrupt();
		this.smsSendReceiveJob = null;
	}
	
	
	private class SmsSendReceiveJob extends Thread {
		
		public SmsSendReceiveJob() {
			super("SmsSendReceiveJobService-SmsSendReceiveJob");
		}
		
		@Override
		public void run() {
			SmsSendReceiveService smsSendReceiveJobInstance = SmsSendReceiveService.this;
			
			app = (RfcxGuardian) getApplication();
			
			try {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);




			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				smsSendReceiveJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
