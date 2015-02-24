package org.rfcx.guardian.intentservice;

import org.rfcx.guardian.RfcxGuardian;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class CarrierCodeTopUp extends IntentService {

	private static final String TAG = "RfcxGuardian-"+CarrierCodeTopUp.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public static final String INTENT_TAG = "org.rfcx.guardian.CARRIER_TOPUP";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.RECEIVE_CARRIER_TOPUP_NOTIFICATIONS";
	
	public CarrierCodeTopUp() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.verboseLog) Log.d(TAG, "Running CarrierCodeTriggerDing");
		
		if (app.isConnected) {
			app.carrierInteraction.currentlyRunningCode = "topup";
			app.triggerService("CarrierCode", true);
		} else {
			Log.d(TAG,"Skipping CarrierCodeTriggerDing attempt, because we're not connected");
		}

	
	}
	
}
