package org.rfcx.guardian.service;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.device.DeviceCPUTuner;
import org.rfcx.guardian.utility.DeviceScreenShot;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class DeviceCPUTunerService extends Service {

	private static final String TAG = "RfcxGuardian-"+DeviceCPUTunerService.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private DeviceCPUTunerSvc deviceCPUTunerSvc;

	private RfcxGuardian app = null;
	private Context context = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceCPUTunerSvc = new DeviceCPUTunerSvc();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		app = (RfcxGuardian) getApplication();
		if (context == null) context = app.getApplicationContext();
		
		app.isRunning_CPUTuner = true;
		try {
			this.deviceCPUTunerSvc.start();
			if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		app.isRunning_CPUTuner = false;
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
				
				(new DeviceCPUTuner()).set(context);
				
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				app.isRunning_CPUTuner = false;
				app.stopService("CPUTuner");
			}
		}
	}

}
