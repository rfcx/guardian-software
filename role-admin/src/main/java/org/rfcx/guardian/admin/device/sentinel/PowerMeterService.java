package org.rfcx.guardian.admin.device.sentinel;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class PowerMeterService extends Service {

	private static final String SERVICE_NAME = "PowerMeter";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "PowerMeterService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private PowerMeterSvc powerMeterSvc;

	private long powerMeterCycleDuration = 5000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.powerMeterSvc = new PowerMeterSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.powerMeterSvc.start();
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
		this.powerMeterSvc.interrupt();
		this.powerMeterSvc = null;
	}
	
	
	private class PowerMeterSvc extends Thread {
		
		public PowerMeterSvc() { super("PowerMeterService-PowerMeterSvc"); }
		
		@Override
		public void run() {
			PowerMeterService powerMeterServiceInstance = PowerMeterService.this;
			
			app = (RfcxGuardian) getApplication();

			while (powerMeterServiceInstance.runFlag) {

				try {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

					if (app.smsMessageDb.dbSmsQueued.getCount() > 0) {

						app.rfcxServiceHandler.triggerService("SmsDispatch", false);

					}

					Thread.sleep(powerMeterCycleDuration);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					powerMeterServiceInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			powerMeterServiceInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
