package admin.device.android.control;

import rfcx.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AirplaneModeEnableService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AirplaneModeEnableService.class);
	
	private static final String SERVICE_NAME = "AirplaneModeEnable";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AirplaneModeEnable airplaneModeEnable;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.airplaneModeEnable = new AirplaneModeEnable();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.airplaneModeEnable.start();
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
		this.airplaneModeEnable.interrupt();
		this.airplaneModeEnable = null;
	}
	
	
	private class AirplaneModeEnable extends Thread {
		
		public AirplaneModeEnable() {
			super("AirplaneModeEnableService-AirplaneModeEnable");
		}
		
		@Override
		public void run() {
			AirplaneModeEnableService airplaneModeEnableInstance = AirplaneModeEnableService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				app.deviceAirplaneMode.setOn(context);
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				airplaneModeEnableInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
