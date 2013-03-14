package org.rfcx.src_api;

import org.rfcx.src_android.RfcxSource;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApiCommService extends Service {

	private static final String TAG = ApiCommService.class.getSimpleName();
	
	private boolean runFlag = false;
	private ApiComm apiComm;

	private RfcxSource rfcxSource = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.apiComm = new ApiComm();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (RfcxSource.VERBOSE) Log.d(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		rfcxSource = (RfcxSource) getApplication();
		rfcxSource.isServiceRunning_ApiComm = true;
		this.apiComm.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		rfcxSource.isServiceRunning_ApiComm = false;
		this.apiComm.interrupt();
		this.apiComm = null;
	}
	
	public boolean isRunning() {
		return runFlag;
	}
	
	private class ApiComm extends Thread {

		public ApiComm() {
			super("ApiCommService-ApiComm");
		}

		@Override
		public void run() {
			ApiCommService apiCommService = ApiCommService.this;
			rfcxSource = (RfcxSource) getApplicationContext();
			while (apiCommService.runFlag) {
				try {
					if (!rfcxSource.airplaneMode.isEnabled(rfcxSource.getApplicationContext())) rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
					Thread.sleep(Math.round(0.6*rfcxSource.apiComm.getConnectivityInterval())*1000);
					rfcxSource.airplaneMode.setOff(rfcxSource.getApplicationContext());
					Thread.sleep(Math.round(0.4*rfcxSource.apiComm.getConnectivityInterval())*1000);
					
				} catch (InterruptedException e) {
					apiCommService.runFlag = false;
					rfcxSource.isServiceRunning_ApiComm = false;
				}
			}
			if (RfcxSource.VERBOSE) Log.d(TAG, "Stopping service: "+TAG);
		}
	}
	
}
