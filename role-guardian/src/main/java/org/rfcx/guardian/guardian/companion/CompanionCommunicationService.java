package org.rfcx.guardian.guardian.companion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.instructions.InstructionsExecutionService;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class CompanionCommunicationService extends Service {

	public static final String SERVICE_NAME = "CompanionCommunication";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CompanionCommunicationService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private CompanionCommunicationSvc companionCommunicationSvc;

	public static final long CYCLE_DURATION = 1500;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.companionCommunicationSvc = new CompanionCommunicationSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.companionCommunicationSvc.start();
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
		this.companionCommunicationSvc.interrupt();
		this.companionCommunicationSvc = null;
	}
	
	
	private class CompanionCommunicationSvc extends Thread {
		
		public CompanionCommunicationSvc() { super("CompanionCommunicationService-CompanionCommunicationSvc"); }
		
		@Override
		public void run() {
			CompanionCommunicationService companionCommunicationInstance = CompanionCommunicationService.this;
			
			app = (RfcxGuardian) getApplication();

			app.apiSocketUtils.updatePingJson();

			while (companionCommunicationInstance.runFlag) {

				try {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

					Thread.sleep(CYCLE_DURATION);

					if (!app.apiSocketUtils.sendSocketPing()) {
						Thread.sleep(3 * CYCLE_DURATION);
					}

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxSvc.setRunState(SERVICE_NAME, false);
					companionCommunicationInstance.runFlag = false;
				}
			}

			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			companionCommunicationInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
