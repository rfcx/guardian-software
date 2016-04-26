package org.rfcx.guardian.api.service;

import org.rfcx.guardian.api.RfcxGuardian;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class ApiCheckInTrigger extends Service {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiCheckInTrigger.class.getSimpleName();
	
	private boolean runFlag = false;
	private ApiCheckIn apiCheckInTrigger;
	
	private RfcxGuardian app = null;
	private Context context = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckInTrigger = new ApiCheckIn();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		
		app = (RfcxGuardian) getApplication();
		context = app.getApplicationContext();
		
		Log.v(TAG, "Starting service: "+TAG);
		
		app.isRunning_ApiCheckInTrigger = true;
		try {
			this.apiCheckInTrigger.start();
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isRunning_ApiCheckInTrigger = false;
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
				Log.d(TAG, "ApiCheckTrigger Period: "+ ( 3 * app.CHECKIN_CYCLE_PAUSE ) +"ms");
				while (apiCheckInTrigger.runFlag) {
					try {
						app.triggerService("ApiCheckIn", false);
						app.apiWebCheckIn.connectivityToggleCheck();
				        Thread.sleep( 3 * app.CHECKIN_CYCLE_PAUSE );

					} catch (Exception e) {
						Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
						apiCheckInTrigger.runFlag = false;
						app.isRunning_ApiCheckInTrigger = false;
					}
				}
				Log.v(TAG, "Stopping service: "+TAG);
				
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
				apiCheckInTrigger.runFlag = false;
				app.isRunning_ApiCheckInTrigger = false;
			}
		}
	}

	
}
