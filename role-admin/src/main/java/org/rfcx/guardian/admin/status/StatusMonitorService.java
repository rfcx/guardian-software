package org.rfcx.guardian.admin.status;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.sms.SmsDispatchService;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class StatusMonitorService extends Service {

	public static final String SERVICE_NAME = "StatusMonitor";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "StatusMonitorService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private StatusMonitorSvc statusMonitorSvc;

	private long statusMonitorCycleDuration = 10000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.statusMonitorSvc = new StatusMonitorSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.statusMonitorSvc.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxSvc.setRunState(SERVICE_NAME, false);
		this.statusMonitorSvc.interrupt();
		this.statusMonitorSvc = null;
	}
	
	
	private class StatusMonitorSvc extends Thread {
		
		public StatusMonitorSvc() { super("StatusMonitorService-StatusMonitorSvc"); }
		
		@Override
		public void run() {
			StatusMonitorService statusMonitorInstance = StatusMonitorService.this;
			
			app = (RfcxGuardian) getApplication();

			while (statusMonitorInstance.runFlag) {

				try {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

					if (app.smsMessageDb.dbSmsQueued.getCount() > 0) {

						app.rfcxSvc.triggerService( SmsDispatchService.SERVICE_NAME, false);

					}

					Thread.sleep(statusMonitorCycleDuration);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxSvc.setRunState(SERVICE_NAME, false);
					statusMonitorInstance.runFlag = false;
				}
			}

			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			statusMonitorInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
