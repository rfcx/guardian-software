package org.rfcx.guardian.service;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.DeviceScreenShot;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class CarrierCodeService extends Service {

	private static final String TAG = "RfcxGuardian-"+CarrierCodeService.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private CarrierCode carrierCode;

	private RfcxGuardian app = null;
	private Context context = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.carrierCode = new CarrierCode();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		app = (RfcxGuardian) getApplication();
		if (context == null) context = app.getApplicationContext();
		
		app.isRunning_CarrierCode = true;
		try {
			this.carrierCode.start();
			Log.v(TAG, "Starting service: "+TAG);
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		app.isRunning_CarrierCode = false;
		this.carrierCode.interrupt();
		this.carrierCode = null;
	}
	
	private class CarrierCode extends Thread {

		public CarrierCode() {
			super("CarrierCodeService-CarrierCode");
		}

		@Override
		public void run() {
			CarrierCodeService carrierCodeService = CarrierCodeService.this;
			try {
				String ussdAction = app.carrierInteraction.currentlyRunningCode;
				
				// if code is set, and carrier codes are enabled in prefs, then run the code sequence
				if ( (ussdAction != null) && (app.sharedPrefs.getBoolean("use_carriercodes",true))) {
					
					app.deviceScreenLock.unLockScreen(context);
					
					String ussdCode = app.getPref("carriercode_"+ussdAction);
					Log.i(TAG, "Running USSD Code: "+ussdAction+" ("+ussdCode+")");
					app.carrierInteraction.submitCode(context, ussdCode);
					
					Thread.sleep(30000);
					(new DeviceScreenShot()).saveScreenShot(context);
					
					String ussdClose = app.getPref("carriercode_"+ussdAction+"_close");
					Log.i(TAG, "Closing USSD Code Response: "+ussdAction+" ("+ussdClose+")");
					app.carrierInteraction.closeResponseDialog(context,app.getPref("carriercode_"+ussdAction+"_close").split(","));
					
				}
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				app.carrierInteraction.currentlyRunningCode = null;
				app.isRunning_CarrierCode = false;
				app.deviceScreenLock.releaseWakeLock();
				app.stopService("CarrierCode");
			}
		}
	}

}
