package org.rfcx.src_monitor;

import org.rfcx.src_android.RfcxSource;

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
		RfcxSource rfcxSource = (RfcxSource) getApplication();
		if (rfcxSource.isServiceRunning_ServiceMonitor) {
			Intent intent = new Intent(INTENT_TAG);
			sendBroadcast(intent, NOTIFICATION_TAG);
			if (rfcxSource.verboseLogging) Log.d(TAG, "Running Service Monitor...");
			TimeOfDay timeOfDay = new TimeOfDay();
			Context context = rfcxSource.getApplicationContext();
			if (timeOfDay.isDataGenerationEnabled(context) || rfcxSource.ignoreOffHours) {
				if (rfcxSource.verboseLogging) Log.d(TAG, "Services should be running.");
				rfcxSource.launchAllServices(context);
			} else {
				if (rfcxSource.verboseLogging) Log.d(TAG, "Services should be suspended.");
				rfcxSource.suspendAllServices(context);
			}
		} else {
			rfcxSource.isServiceRunning_ServiceMonitor = true;
		}
	}
	
	
}
