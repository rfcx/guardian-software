package org.rfcx.guardian.guardian.instructions;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class InstructionsCycleService extends Service {

	private static final String SERVICE_NAME = "InstructionsCycle";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstructionsCycleService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private InstructionsCycleSvc instructionsCycleSvc;

	public static final long CYCLE_DURATION = 5000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.instructionsCycleSvc = new InstructionsCycleSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.instructionsCycleSvc.start();
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
		this.instructionsCycleSvc.interrupt();
		this.instructionsCycleSvc = null;
	}
	
	
	private class InstructionsCycleSvc extends Thread {
		
		public InstructionsCycleSvc() { super("InstructionsCycleService-InstructionsCycleSvc"); }
		
		@Override
		public void run() {
			InstructionsCycleService instructionsCycleInstance = InstructionsCycleService.this;
			
			app = (RfcxGuardian) getApplication();

			while (instructionsCycleInstance.runFlag) {

				try {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

					if (app.instructionsDb.dbQueuedInstructions.getCount() > 0) {

						app.rfcxServiceHandler.triggerService("InstructionsExecution", false);

					}

					Thread.sleep(CYCLE_DURATION);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					instructionsCycleInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			instructionsCycleInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
