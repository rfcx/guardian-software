package org.rfcx.guardian.service;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.telecom.CarrierInteraction;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class CarrierCodeService extends Service {

	private static final String TAG = CarrierCodeService.class.getSimpleName();
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
			if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
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
			CarrierInteraction carrierInteraction = new CarrierInteraction();
			try {

				if (app.verboseLog) { Log.d(TAG, "Pre-approving prompt close code."); }
				carrierInteraction.closeResponseDialog(app.getPrefString("carriercode_topup_close").split(","));
				if (app.verboseLog) { Log.d(TAG, "Executing USSD Code"); }
				carrierInteraction.submitCode(context, app.getPrefString("carriercode_topup"));
				Thread.sleep(15000);
				if (app.verboseLog) { Log.d(TAG, "Closing USSD Code Feedback"); }
				carrierInteraction.closeResponseDialog(app.getPrefString("carriercode_topup_close").split(","));
	
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				app.isRunning_CarrierCode = false;
				app.stopService("CarrierCode");
			}
		}
	}

}
