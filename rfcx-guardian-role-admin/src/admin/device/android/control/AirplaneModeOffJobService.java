package admin.device.android.control;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AirplaneModeOffJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AirplaneModeOffJobService.class);
	
	private static final String SERVICE_NAME = "AirplaneModeOffJob";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AirplaneModeOffJob airplaneModeOffJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.airplaneModeOffJob = new AirplaneModeOffJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.airplaneModeOffJob.start();
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
		this.airplaneModeOffJob.interrupt();
		this.airplaneModeOffJob = null;
	}
	
	
	private class AirplaneModeOffJob extends Thread {
		
		public AirplaneModeOffJob() {
			super("AirplaneModeOffJobService-AirplaneModeOffJob");
		}
		
		@Override
		public void run() {
			AirplaneModeOffJobService airplaneModeOffJobInstance = AirplaneModeOffJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				app.deviceAirplaneMode.setOff(context);
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				airplaneModeOffJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
