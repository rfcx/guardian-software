package org.rfcx.src_api;

import org.rfcx.rfcx_src_android.RfcxSource;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApiCommService extends Service {

	private static final String TAG = ApiCommService.class.getSimpleName();

	static final int DELAY = 300000;
	static final int INNER_DELAY = 60000;
	
	private boolean runFlag = false;
	private ApiComm apiComm;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.apiComm = new ApiComm();
		Log.d(TAG, "onCreated()");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		this.apiComm.start();
		Log.d(TAG, "onStarted()");
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		this.apiComm.interrupt();
		this.apiComm = null;
		Log.d(TAG, "onDestroyed()");
	}
	
	private class ApiComm extends Thread {

		public ApiComm() {
			super("ApiCommService-ApiComm");
		}

		@Override
		public void run() {
			ApiCommService apiCommService = ApiCommService.this;
			RfcxSource rfcxSource = (RfcxSource) getApplicationContext();
			while (apiCommService.runFlag) {
				if (RfcxSource.verboseLog()) { Log.d(TAG, "ApiCommService running"); }
				try {
					if (RfcxSource.verboseLog()) { Log.d(TAG, "Enabling network... (setOff)"); }
					rfcxSource.airplaneMode.setOff(rfcxSource.getApplicationContext());
					Thread.sleep(INNER_DELAY);
					if (RfcxSource.verboseLog()) { Log.d(TAG, "Disabling network... (setOn)"); }
					rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					apiCommService.runFlag = false;
				}
			}
		}
	}
	
}
