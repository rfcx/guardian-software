package org.rfcx.guardian.api.service;

import org.rfcx.guardian.api.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApiCheckInTrigger extends Service {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiCheckInTrigger.class.getSimpleName();
	
	private static final String SERVICE_NAME = "ApiCheckInTrigger";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApiCheckIn apiCheckInTrigger;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckInTrigger = new ApiCheckIn();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.apiCheckInTrigger.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(TAG, e);
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.apiCheckInTrigger.interrupt();
		this.apiCheckInTrigger = null;
	}
	
	
	private class ApiCheckIn extends Thread {
		
		public ApiCheckIn() {
			super("ApiCheckInTrigger-ApiCheckIn");
		}
		
		@Override
		public void run() {
			ApiCheckInTrigger apiCheckInTrigger = ApiCheckInTrigger.this;
			
			app = (RfcxGuardian) getApplication();
			
			try {
				Log.d(TAG, "ApiCheckTrigger Period: "+ ( 3 * app.rfcxPrefs.getPrefAsInt("checkin_cycle_pause") ) +"ms");
				while (apiCheckInTrigger.runFlag) {
					try {
						app.rfcxServiceHandler.triggerService("ApiCheckIn", false);
						app.apiWebCheckIn.connectivityToggleCheck();
				        Thread.sleep( 3 * app.rfcxPrefs.getPrefAsInt("checkin_cycle_pause") );

					} catch (Exception e) {
						RfcxLog.logExc(TAG, e);
						app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
						apiCheckInTrigger.runFlag = false;
					}
				}
				Log.v(TAG, "Stopping service: "+TAG);
				
			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				apiCheckInTrigger.runFlag = false;
			}
		}
	}

	
}
