package org.rfcx.guardian.installer.service;

import org.rfcx.guardian.installer.RfcxGuardian;
import org.rfcx.guardian.installer.device.DeviceCPUTuner;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class DeviceCPUTunerService extends Service {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceCPUTunerService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "CPUTuner";

	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceCPUTunerSvc deviceCPUTunerSvc;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceCPUTunerSvc = new DeviceCPUTunerSvc();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceCPUTunerSvc.start();
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
		this.deviceCPUTunerSvc.interrupt();
		this.deviceCPUTunerSvc = null;
	}
	
	private class DeviceCPUTunerSvc extends Thread {

		public DeviceCPUTunerSvc() {
			super("DeviceCPUTunerService-DeviceCPUTunerSvc");
		}

		@Override
		public void run() {
			DeviceCPUTunerService deviceCPUTunerService = DeviceCPUTunerService.this;
			try {
				
				(new DeviceCPUTuner()).set(app.getApplicationContext());
				
			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
			} finally {
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

}
