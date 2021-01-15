package org.rfcx.guardian.admin.sbd;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SbdDispatchCycleService extends Service {

	public static final String SERVICE_NAME = "SbdDispatchCycle";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SbdDispatchCycleService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private SbdDispatchCycleSvc sbdDispatchCycleSvc;

	private long sbdDispatchCycleDuration = 5000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.sbdDispatchCycleSvc = new SbdDispatchCycleSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.sbdDispatchCycleSvc.start();
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
		this.sbdDispatchCycleSvc.interrupt();
		this.sbdDispatchCycleSvc = null;
	}
	
	
	private class SbdDispatchCycleSvc extends Thread {
		
		public SbdDispatchCycleSvc() { super("SbdDispatchCycleService-SbdDispatchCycleSvc"); }
		
		@Override
		public void run() {
			SbdDispatchCycleService sbdDispatchCycleInstance = SbdDispatchCycleService.this;
			
			app = (RfcxGuardian) getApplication();

			while (sbdDispatchCycleInstance.runFlag) {

				try {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

					if (app.sbdMessageDb.dbSbdQueued.getCount() > 0) {

						app.rfcxSvc.triggerService( SbdDispatchService.SERVICE_NAME, false);

					}

					Thread.sleep(sbdDispatchCycleDuration);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxSvc.setRunState(SERVICE_NAME, false);
					sbdDispatchCycleInstance.runFlag = false;
				}
			}

			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			sbdDispatchCycleInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
