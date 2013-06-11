package org.rfcx.src_sleep;

import org.rfcx.src_android.RfcxSource;
import org.rfcx.src_api.ApiComm;
import org.rfcx.src_api.ApiCommService;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class SleepMonitorService extends IntentService {
	
	private static final String TAG = SleepMonitorService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.src_android.SRC_SLEEP_MONITOR";
	public static final String NOTIFICATION_TAG = "org.rfcx.src_android.RECEIVE_SLEEP_MONITOR_NOTIFICATIONS";
	
	public SleepMonitorService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxSource rfcxSource = (RfcxSource) getApplication();
		if (rfcxSource.isServiceRunning_SleepTrigger) {
			Intent intent = new Intent(INTENT_TAG);
			sendBroadcast(intent, NOTIFICATION_TAG);
			
			SleepState sleepState = new SleepState();

		} else {
			Log.d(TAG, "Skipping (first run)");
			rfcxSource.isServiceRunning_SleepTrigger = true;
		}
	}
}
