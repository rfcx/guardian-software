package org.rfcx.src_api;

import org.rfcx.src_android.RfcxSource;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApiCommService extends Service {

	private static final String TAG = ApiCommService.class.getSimpleName();
	
	private boolean runFlag = false;
	private ApiCommSvc apiCommSvc;

	private RfcxSource rfcxSource = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCommSvc = new ApiCommSvc();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (RfcxSource.VERBOSE) Log.d(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		rfcxSource = (RfcxSource) getApplication();
		rfcxSource.isServiceRunning_ApiComm = true;
		this.apiCommSvc.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		rfcxSource.isServiceRunning_ApiComm = false;
		this.apiCommSvc.interrupt();
		this.apiCommSvc = null;
	}
	
	public boolean isRunning() {
		return runFlag;
	}
	
	private class ApiCommSvc extends Thread {

		public ApiCommSvc() {
			super("ApiCommService-ApiCommSvc");
		}

		@Override
		public void run() {
			ApiCommService apiCommService = ApiCommService.this;
			rfcxSource = (RfcxSource) getApplicationContext();
			while (apiCommService.runFlag) {
				try {
					if (!rfcxSource.airplaneMode.isEnabled(rfcxSource.getApplicationContext())) rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
					Thread.sleep((ApiComm.CONNECTIVITY_INTERVAL-ApiComm.CONNECTIVITY_TIMEOUT)*1000);
					rfcxSource.airplaneMode.setOff(rfcxSource.getApplicationContext());
					Thread.sleep(ApiComm.CONNECTIVITY_TIMEOUT*1000);
					
				} catch (InterruptedException e) {
					apiCommService.runFlag = false;
					rfcxSource.isServiceRunning_ApiComm = false;
				}
			}
			if (RfcxSource.VERBOSE) Log.d(TAG, "Stopping service: "+TAG);
		}
	}
	
}
