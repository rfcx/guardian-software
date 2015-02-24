package org.rfcx.guardian.intentservice;

import org.rfcx.guardian.RfcxGuardian;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class CarrierCodeBalance extends IntentService {

	private static final String TAG = "RfcxGuardian-"+CarrierCodeBalance.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public static final String INTENT_TAG = "org.rfcx.guardian.CARRIER_BALANCE";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.RECEIVE_CARRIER_BALANCE_NOTIFICATIONS";
	
	public CarrierCodeBalance() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.verboseLog) Log.d(TAG, "Running CarrierCodeTriggerBalance");
		
		if (app.isConnected) {
			app.carrierInteraction.currentlyRunningCode = "balance";
			app.triggerService("CarrierCode", true);
		} else {
			Log.d(TAG,"Skipping CarrierCodeTriggerBalance attempt, because we're not connected");
		}

	
	}
	
}
