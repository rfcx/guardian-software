package org.rfcx.guardian.intentservice;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.TimeOfDay;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceMonitorIntentService extends IntentService {
	
	private static final String TAG = ServiceMonitorIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.SERVICE_MONITOR";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.RECEIVE_SERVICE_MONITOR_NOTIFICATIONS";
	
	public ServiceMonitorIntentService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		if (app.verboseLog) Log.d(TAG, "Running Service Monitor...");
		
		if (app.isRunning_ServiceMonitor) {
			
			// logic about whether services should be allowed (and/or which ones...)
			
			
		} else {
			// the Monitor logic won't run the first time the intent service is fired
			app.isRunning_ServiceMonitor = true;
		}
//		if (app.isCrisisModeEnabled) {
//			if (app.verboseLog) Log.d(TAG, "Crisis mode enabled! Making sure services are disabled...");
//			app.suspendAllServices(context);
//		} else if (app.isRunning_ServiceMonitor) {
//			TimeOfDay timeOfDay = new TimeOfDay();
//			if (timeOfDay.isDataGenerationEnabled(context) || app.ignoreOffHours) {
//				if (app.verboseLog) Log.d(TAG, "Services should be running.");
//				app.launchAllServices(context);
//			} else {
//				if (app.verboseLog) Log.d(TAG, "Services should be suspended.");
//				app.suspendAllServices(context);
//			}
//		} else {
//			app.isRunning_ServiceMonitor = true;
//		}
	}
	
	
}
