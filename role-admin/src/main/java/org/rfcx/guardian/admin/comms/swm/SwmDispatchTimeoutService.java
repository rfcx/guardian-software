package org.rfcx.guardian.admin.comms.swm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SwmDispatchTimeoutService extends Service {

	public static final String SERVICE_NAME = "SwmDispatchTimeout";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmDispatchTimeoutService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private SwmDispatchTimeoutSvc swmDispatchTimeoutSvc;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.swmDispatchTimeoutSvc = new SwmDispatchTimeoutSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.swmDispatchTimeoutSvc.start();
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
		this.swmDispatchTimeoutSvc.interrupt();
		this.swmDispatchTimeoutSvc = null;
	}
	
	
	private class SwmDispatchTimeoutSvc extends Thread {
		
		public SwmDispatchTimeoutSvc() { super("SwmDispatchTimeoutService-SwmDispatchTimeoutSvc"); }
		
		@Override
		public void run() {
			SwmDispatchTimeoutService swmDispatchTimeoutInstance = SwmDispatchTimeoutService.this;
			
			app = (RfcxGuardian) getApplication();

			int checkIntervalCount = Math.round( ( SwmUtils.sendCmdTimeout + ( 3 * SwmUtils.prepCmdTimeout) ) / 667 );

			try {

				app.rfcxSvc.reportAsActive(SERVICE_NAME);

				for (int i = 0; i <= checkIntervalCount; i++) {
					if (app.swmUtils.isInFlight) {
						Thread.sleep(667);
						if (i == checkIntervalCount) {
							Log.e(logTag, "Timeout Reached for SWM Send. Killing serial processes...");
							ShellCommands.killProcessesByIds(app.swmUtils.findRunningSerialProcessIds());
						}
					} else {
						break;
					}
				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxSvc.setRunState(SERVICE_NAME, false);
				swmDispatchTimeoutInstance.runFlag = false;
			}

			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			swmDispatchTimeoutInstance.runFlag = false;
		//	Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
