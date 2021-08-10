package org.rfcx.guardian.guardian.companion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class CompanionSocketService extends Service {

	public static final String SERVICE_NAME = "CompanionSocket";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CompanionSocketService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private CompanionSocketSvc companionSocketSvc;

	private static final long CYCLE_DURATION = 1250;

	private static final int ifSendFailsThenExtendLoopByAFactorOf = 6;
	private static final int maxSendFailureThreshold = 12;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.companionSocketSvc = new CompanionSocketSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.companionSocketSvc.start();
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
		this.companionSocketSvc.interrupt();
		this.companionSocketSvc = null;
	}
	
	
	private class CompanionSocketSvc extends Thread {
		
		public CompanionSocketSvc() { super("CompanionSocketService-CompanionSocketSvc"); }
		
		@Override
		public void run() {
			CompanionSocketService companionSocketInstance = CompanionSocketService.this;
			
			app = (RfcxGuardian) getApplication();

			if (app.apiSocketUtils.isSocketServerEnabled(true)) {

				int currFailureThreshold = maxSendFailureThreshold +1;

				while (companionSocketInstance.runFlag) {

					try {

						app.rfcxSvc.reportAsActive(SERVICE_NAME);

						if (currFailureThreshold >= maxSendFailureThreshold) {
							if (currFailureThreshold == maxSendFailureThreshold) { Log.v(logTag, "Restarting Socket Server..."); }
							app.apiSocketUtils.stopServer();
							app.apiSocketUtils.startServer();
							Thread.sleep( CYCLE_DURATION );
							currFailureThreshold = 0;
							app.apiSocketUtils.updatePingJson();
						}

						if (app.apiSocketUtils.sendSocketPing()) {
							Thread.sleep( CYCLE_DURATION );
							currFailureThreshold = 0;
							app.apiSocketUtils.updatePingJson();
						} else {
							Thread.sleep(ifSendFailsThenExtendLoopByAFactorOf * CYCLE_DURATION );
							currFailureThreshold++;
						}


					} catch (Exception e) {
						RfcxLog.logExc(logTag, e);
						app.rfcxSvc.setRunState(SERVICE_NAME, false);
						companionSocketInstance.runFlag = false;
					}
				}
			}

			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			companionSocketInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
