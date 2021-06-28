package org.rfcx.guardian.admin.comms.swm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.comms.sbd.SbdDispatchService;
import org.rfcx.guardian.admin.comms.sbd.SbdUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SwmDispatchCycleService extends Service {

	public static final String SERVICE_NAME = "SwmDispatchCycle";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmDispatchCycleService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private SwmDispatchCycleSvc swmDispatchCycleSvc;

	private final long swmDispatchCycleDuration = 15000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.swmDispatchCycleSvc = new SwmDispatchCycleSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.swmDispatchCycleSvc.start();
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
		this.swmDispatchCycleSvc.interrupt();
		this.swmDispatchCycleSvc = null;
	}
	
	
	private class SwmDispatchCycleSvc extends Thread {
		
		public SwmDispatchCycleSvc() { super("SwmDispatchCycleService-SwmDispatchCycleSvc"); }
		
		@Override
		public void run() {
			SwmDispatchCycleService swmDispatchCycleInstance = SwmDispatchCycleService.this;
			
			app = (RfcxGuardian) getApplication();

			int cyclesSinceLastActivity = 0;
			int powerOffAfterThisManyInactiveCycles = 6;

			ShellCommands.killProcessesByIds(app.swmUtils.findRunningSerialProcessIds());

			while (swmDispatchCycleInstance.runFlag) {

				try {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

					Thread.sleep(swmDispatchCycleDuration);

					if (app.swmMessageDb.dbSwmQueued.getCount() == 0) {

						// let's add something that checks and eventually powers off the satellite board if not used for a little while
						if (cyclesSinceLastActivity == powerOffAfterThisManyInactiveCycles) {
							app.swmUtils.setPower(false);
						}
						cyclesSinceLastActivity++;


					} else if (!app.swmUtils.isInFlight) {

						boolean isAbleToSend = app.swmUtils.isPowerOn();

						if (!isAbleToSend) {
							Log.i(logTag, "Iridium board is powered OFF. Turning power ON...");
							app.swmUtils.setPower(true);
							isAbleToSend = app.swmUtils.isPowerOn();
						}

						if (!isAbleToSend) {
							Log.e(logTag, "Iridium board is STILL powered off. Unable to proceed with SWM send...");
						} else if (!app.swmUtils.isNetworkAvailable()) {
							Log.e(logTag, "Iridium Network is not available. Unable to proceed with SWM send...");
						} else {
							app.rfcxSvc.triggerOrForceReTriggerIfTimedOut(SwmDispatchService.SERVICE_NAME, Math.round( 1.5 * SwmUtils.sendCmdTimeout) );
							cyclesSinceLastActivity = 0;
						}

					} else {

						app.rfcxSvc.triggerOrForceReTriggerIfTimedOut(SwmDispatchService.SERVICE_NAME, Math.round( 1.5 * SwmUtils.sendCmdTimeout) );
						cyclesSinceLastActivity = 0;

					}

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxSvc.setRunState(SERVICE_NAME, false);
					swmDispatchCycleInstance.runFlag = false;
				}
			}

			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			swmDispatchCycleInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
