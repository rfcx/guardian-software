package org.rfcx.guardian.guardian.instructions;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class InstructionsSchedulerService extends Service {

	public static final String SERVICE_NAME = "InstructionsScheduler";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstructionsSchedulerService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private InstructionsSchedulerSvc instructionsSchedulerSvc;

	public static final long CYCLE_DURATION = 60 * 1000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.instructionsSchedulerSvc = new InstructionsSchedulerSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.instructionsSchedulerSvc.start();
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
		this.instructionsSchedulerSvc.interrupt();
		this.instructionsSchedulerSvc = null;
	}
	
	
	private class InstructionsSchedulerSvc extends Thread {
		
		public InstructionsSchedulerSvc() { super("InstructionsSchedulerService-InstructionsSchedulerSvc"); }
		
		@Override
		public void run() {
			InstructionsSchedulerService instructionsSchedulerInstance = InstructionsSchedulerService.this;
			
			app = (RfcxGuardian) getApplication();

			while (instructionsSchedulerInstance.runFlag) {

				try {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

//					if (app.instructionsDb.dbQueued.getCount() > 0) {
//
//						app.rfcxSvc.triggerService( InstructionsExecutionService.SERVICE_NAME, false);
//
//					}

					Thread.sleep(CYCLE_DURATION);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxSvc.setRunState(SERVICE_NAME, false);
					instructionsSchedulerInstance.runFlag = false;
				}
			}

			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			instructionsSchedulerInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
