package org.rfcx.guardian.admin.device.android.control;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AirplaneModeToggleService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AirplaneModeToggleService.class);
	
	private static final String SERVICE_NAME = "AirplaneModeToggle";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AirplaneModeToggle airplaneModeToggle;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.airplaneModeToggle = new AirplaneModeToggle();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.airplaneModeToggle.start();
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
		this.airplaneModeToggle.interrupt();
		this.airplaneModeToggle = null;
	}
	
	
	private class AirplaneModeToggle extends Thread {
		
		public AirplaneModeToggle() {
			super("AirplaneModeToggleService-AirplaneModeToggle");
		}
		
		@Override
		public void run() {
			AirplaneModeToggleService airplaneModeToggleInstance = AirplaneModeToggleService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				app.deviceAirplaneMode.setOff(context);
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				airplaneModeToggleInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
