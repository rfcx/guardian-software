package org.rfcx.guardian.intentservice;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.TimeOfDay;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CarrierCodeTriggerIntentService extends IntentService {

	private static final String TAG = "RfcxGuardian-"+CarrierCodeTriggerIntentService.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public static final String INTENT_TAG = "org.rfcx.guardian.CARRIER_CODE_TRIGGER";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.RECEIVE_CARRIER_CODE_TRIGGER_NOTIFICATIONS";
	
	public CarrierCodeTriggerIntentService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.verboseLog) Log.d(TAG, "Running CarrierCodeTrigger");
		
		if (app.isRunning_CarrierCodeTrigger) {
			if (app.isConnected) {
				app.triggerService("CarrierCode", true);
			} else {
				Log.d(TAG,"Skipping CarrierCodeTrigger attempt, because we're not connected");
			}
		} else {
			// the Monitor logic won't run the first time the intent service is fired
			app.isRunning_CarrierCodeTrigger = true;
		}
	
	}
	
}
