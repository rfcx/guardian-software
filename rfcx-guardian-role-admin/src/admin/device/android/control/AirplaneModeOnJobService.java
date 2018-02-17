package admin.device.android.control;

import rfcx.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AirplaneModeOnJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AirplaneModeOnJobService.class);
	
	private static final String SERVICE_NAME = "AirplaneModeOnJob";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AirplaneModeOnJob airplaneModeOnJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.airplaneModeOnJob = new AirplaneModeOnJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.airplaneModeOnJob.start();
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
		this.airplaneModeOnJob.interrupt();
		this.airplaneModeOnJob = null;
	}
	
	
	private class AirplaneModeOnJob extends Thread {
		
		public AirplaneModeOnJob() {
			super("AirplaneModeOnJobService-AirplaneModeOnJob");
		}
		
		@Override
		public void run() {
			AirplaneModeOnJobService airplaneModeOnJobInstance = AirplaneModeOnJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				app.deviceAirplaneMode.setOn(context);
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				airplaneModeOnJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
