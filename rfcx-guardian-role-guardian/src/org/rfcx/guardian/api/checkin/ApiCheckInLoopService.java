package org.rfcx.guardian.api.checkin;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApiCheckInLoopService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInLoopService.class);
	
	private static final String SERVICE_NAME = "ApiCheckInLoop";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApiCheckInLoop apiCheckInLoop;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckInLoop = new ApiCheckInLoop();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.apiCheckInLoop.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.apiCheckInLoop.interrupt();
		this.apiCheckInLoop = null;
	}
	
	
	private class ApiCheckInLoop extends Thread {
		
		public ApiCheckInLoop() {
			super("ApiCheckInLoopService-ApiCheckInLoop");
		}
		
		@Override
		public void run() {
			ApiCheckInLoopService apiCheckInLoopInstance = ApiCheckInLoopService.this;
			
			app = (RfcxGuardian) getApplication();

			long apiCheckInLoopCyclePause = (long) (3 * app.rfcxPrefs.getPrefAsInt("checkin_cycle_pause"));
			long apiCheckInLoopTimeOut = (long) (3 * app.rfcxPrefs.getPrefAsInt("audio_cycle_duration"));
			
			try {
				Log.d(logTag, "ApiCheckInLoop Period: "+ apiCheckInLoopCyclePause +"ms");
				while (apiCheckInLoopInstance.runFlag) {
					
					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
					try {
				        Thread.sleep(apiCheckInLoopCyclePause);
				        app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("ApiCheckInJob", apiCheckInLoopTimeOut);
						app.apiCheckInUtils.connectivityToggleCheck();

					} catch (Exception e) {
						RfcxLog.logExc(logTag, e);
						app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
						apiCheckInLoopInstance.runFlag = false;
					}
				}
				Log.v(logTag, "Stopping service: "+logTag);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				apiCheckInLoopInstance.runFlag = false;
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				apiCheckInLoopInstance.runFlag = false;
			}
		}
	}

	
}
