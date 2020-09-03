package org.rfcx.guardian.admin.sms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SmsDispatchCycleService extends Service {

	private static final String SERVICE_NAME = "SmsDispatchCycle";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SmsDispatchCycleService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private SmsDispatchCycleSvc smsDispatchCycleSvc;

	private long smsDispatchCycleDuration = 5000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.smsDispatchCycleSvc = new SmsDispatchCycleSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.smsDispatchCycleSvc.start();
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
		this.smsDispatchCycleSvc.interrupt();
		this.smsDispatchCycleSvc = null;
	}
	
	
	private class SmsDispatchCycleSvc extends Thread {
		
		public SmsDispatchCycleSvc() { super("SmsDispatchCycleService-SmsDispatchCycleSvc"); }
		
		@Override
		public void run() {
			SmsDispatchCycleService smsDispatchCycleInstance = SmsDispatchCycleService.this;
			
			app = (RfcxGuardian) getApplication();

			while (smsDispatchCycleInstance.runFlag) {

				try {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

					if (app.smsMessageDb.dbSmsQueued.getCount() > 0) {

						app.rfcxServiceHandler.triggerService("SmsDispatch", false);

					}

					Thread.sleep(smsDispatchCycleDuration);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					smsDispatchCycleInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			smsDispatchCycleInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
