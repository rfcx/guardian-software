package org.rfcx.guardian.intentservice;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.device.DeviceCPUTuner;
import org.rfcx.guardian.utility.TimeOfDay;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceMonitor extends IntentService {
	
	private static final String TAG = "RfcxGuardian-"+ServiceMonitor.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.SERVICE_MONITOR";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.RECEIVE_SERVICE_MONITOR_NOTIFICATIONS";
	
	public ServiceMonitor() {
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
			
			app.triggerService("AudioCapture", false);
			
			app.triggerService("ApiCheckInTrigger",
				// if a check-in hasn't been triggered for as long as the first connectivity threshold,
				// then assume that the CheckInTrigger service is actually not running, and force restart it
				(System.currentTimeMillis() > (app.apiCore.requestSendStart.getTime() + app.apiCore.connectivityToggleThresholds[0]*60*1000))
				);
			
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
