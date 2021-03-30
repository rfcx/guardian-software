package org.rfcx.guardian.admin.sbd;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SbdDispatchTimeoutService extends Service {

	public static final String SERVICE_NAME = "SbdDispatchTimeout";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SbdDispatchTimeoutService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private SbdDispatchTimeoutSvc sbdDispatchTimeoutSvc;

	private long sbdDispatchTimeoutDuration = 15000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.sbdDispatchTimeoutSvc = new SbdDispatchTimeoutSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.sbdDispatchTimeoutSvc.start();
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
		this.sbdDispatchTimeoutSvc.interrupt();
		this.sbdDispatchTimeoutSvc = null;
	}
	
	
	private class SbdDispatchTimeoutSvc extends Thread {
		
		public SbdDispatchTimeoutSvc() { super("SbdDispatchTimeoutService-SbdDispatchTimeoutSvc"); }
		
		@Override
		public void run() {
			SbdDispatchTimeoutService sbdDispatchTimeoutInstance = SbdDispatchTimeoutService.this;
			
			app = (RfcxGuardian) getApplication();

			while (sbdDispatchTimeoutInstance.runFlag) {

				try {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

//					if (app.sbdMessageDb.dbSbdQueued.getCount() == 0) {
//
//						// let's add something that checks and eventually powers off the satellite board if not used for a little while
//
//					} else {
//
//						Log.i(logTag, "SBD Message is queued for dispatch. Attempting to send...");
//
//						boolean isAbleToSend = app.sbdUtils.isPowerOn();
//
//						if (!isAbleToSend) {
//							Log.i(logTag, "Iridium board is powered OFF. Turning power ON...");
//							app.sbdUtils.setPower(true);
//							isAbleToSend = app.sbdUtils.isPowerOn();
//						}
//
//						if (!isAbleToSend) {
//							Log.e(logTag, "Iridium board is STILL powered off. Unable to proceed with SBD send...");
//						} else if (!app.sbdUtils.isNetworkAvailable()) {
//							Log.e(logTag, "Iridium Network is not available. Unable to proceed with SBD send...");
//						} else {
//							Log.i(logTag, "Iridium board is powered ON and network is available. Proceeding with SBD send...");
//							app.rfcxSvc.triggerService(SbdDispatchService.SERVICE_NAME, false);
//
//							// Adding extra delay
//							Thread.sleep(3 * sbdDispatchTimeoutDuration);
//						}
//
//					}

					Thread.sleep(sbdDispatchTimeoutDuration);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxSvc.setRunState(SERVICE_NAME, false);
					sbdDispatchTimeoutInstance.runFlag = false;
				}
			}

			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			sbdDispatchTimeoutInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
