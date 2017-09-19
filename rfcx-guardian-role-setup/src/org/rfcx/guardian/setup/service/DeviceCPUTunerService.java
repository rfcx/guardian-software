package org.rfcx.guardian.setup.service;

import org.rfcx.guardian.setup.RfcxGuardian;
import org.rfcx.guardian.utility.install.DeviceCPUTuner;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DeviceCPUTunerService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceCPUTunerService.class);
	
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
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceCPUTunerSvc.start();
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
				
				(new DeviceCPUTuner(app.getApplicationContext(), RfcxGuardian.APP_ROLE))
					.writeConfiguration(
						app.rfcxPrefs.getPrefAsInt("cputuner_freq_min"),
		        		app.rfcxPrefs.getPrefAsInt("cputuner_freq_max"),
		        		app.rfcxPrefs.getPrefAsInt("cputuner_governor_up"),
		        		app.rfcxPrefs.getPrefAsInt("cputuner_governor_down")
		        	);
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

}
