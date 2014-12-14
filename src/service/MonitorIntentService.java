package service;

import org.rfcx.src_android.RfcxGuardian;

import utility.TimeOfDay;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MonitorIntentService extends IntentService {
	
	private static final String TAG = MonitorIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.src_android.SRC_SLEEP_MONITOR";
	public static final String NOTIFICATION_TAG = "org.rfcx.src_android.RECEIVE_SLEEP_MONITOR_NOTIFICATIONS";
	
	public MonitorIntentService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		if (app.verboseLogging) Log.d(TAG, "Running Service Monitor...");
		
		if (app.isCrisisModeEnabled) {
			if (app.verboseLogging) Log.d(TAG, "Crisis mode enabled! Making sure services are disabled...");
			app.suspendAllServices(context);
		} else if (app.isServiceRunning_ServiceMonitor) {
			TimeOfDay timeOfDay = new TimeOfDay();
			if (timeOfDay.isDataGenerationEnabled(context) || app.ignoreOffHours) {
				if (app.verboseLogging) Log.d(TAG, "Services should be running.");
				app.launchAllServices(context);
			} else {
				if (app.verboseLogging) Log.d(TAG, "Services should be suspended.");
				app.suspendAllServices(context);
			}
		} else {
			app.isServiceRunning_ServiceMonitor = true;
		}
	}
	
	
}
