package org.rfcx.guardian.service;

import org.rfcx.guardian.RfcxGuardian;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class CarrierUssdService extends Service {

	private static final String TAG = CarrierUssdService.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private CarrierUssd carrierUssd;

	private RfcxGuardian app = null;
	private Context context = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.carrierUssd = new CarrierUssd();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		app = (RfcxGuardian) getApplication();
		if (context == null) context = app.getApplicationContext();
		
		app.isRunning_CarrierUssd = true;
		try {
			this.carrierUssd.start();
			if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		app.isRunning_CarrierUssd = false;
		this.carrierUssd.interrupt();
		this.carrierUssd = null;
	}
	
	private class CarrierUssd extends Thread {

		public CarrierUssd() {
			super("CarrierUssdService-CarrierUssd");
		}

		@Override
		public void run() {
			CarrierUssdService carrierUssdService = CarrierUssdService.this;
		//	HttpGet httpGet = new HttpGet();
			try {
				
				Log.d(TAG, "Running the service!!!!");
				
				// DO SOMETHING
				
				//				if (app.apiCore.apiCheckVersionEndpoint != null) {
//					String getUrl =	(((app.getPref("api_domain")!=null) ? app.getPref("api_domain") : "https://api.rfcx.org")
//									+ app.apiCore.apiCheckVersionEndpoint
//									+ "?nocache="+Calendar.getInstance().getTimeInMillis());
//					
//					JSONObject jsonResponse = httpGet.getAsJson(getUrl);
//					if (app.verboseLog) { 
//						Log.d(TAG, jsonResponse.toJSONString());
//					}
//					app.apiCore.apiCheckVersionFollowUp(app,jsonResponse);
//				} else {
//					Log.d(TAG, "Cancelled because apiCheckVersionEndpoint is null...");
//				}
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				app.isRunning_CarrierUssd = false;
				app.stopService("CarrierUssd");
			}
		}
	}

}
