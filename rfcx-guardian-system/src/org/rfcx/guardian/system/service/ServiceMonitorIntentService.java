package org.rfcx.guardian.system.service;


import org.rfcx.guardian.system.RfcxGuardian;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceMonitorIntentService extends IntentService {
	
	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.Constants.ROLE_NAME+"-"+ServiceMonitorIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+org.rfcx.guardian.utility.Constants.ROLE_NAME.toLowerCase()+".SERVICE_MONITOR";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+org.rfcx.guardian.utility.Constants.ROLE_NAME.toLowerCase()+".RECEIVE_SERVICE_MONITOR_NOTIFICATIONS";
	
	public ServiceMonitorIntentService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		Log.v(TAG, "Running Service Monitor...");
		
		if (app.isRunning_ServiceMonitor) {
			
			app.triggerService("DeviceState", false);
			
		} else {
			// the Monitor logic won't run the first time the intent service is fired
			app.isRunning_ServiceMonitor = true;
			
		}
	}
	
	
}
