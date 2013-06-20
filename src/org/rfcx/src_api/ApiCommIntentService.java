package org.rfcx.src_api;

import org.rfcx.src_android.RfcxSource;
import org.rfcx.src_monitor.TimeOfDay;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ApiCommIntentService extends IntentService {

	private static final String TAG = ApiCommIntentService.class.getSimpleName();
	
	public static final String SRC_API_COMM = "org.rfcx.src_android.SRC_API_COMM";
	public static final String RECEIVE_API_COMM_NOTIFICATIONS = "org.rfcx.src_android.RECEIVE_API_COMM_NOTIFICATIONS";
	
	public ApiCommIntentService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxSource app = (RfcxSource) getApplication();
		Context context = app.getApplicationContext();
		TimeOfDay timeOfDay = new TimeOfDay();
		if (app.isServiceRunning_ApiComm && timeOfDay.isDataGenerationEnabled(context)) {
			app.airplaneMode.setOff(app.getApplicationContext());	
			Intent intent = new Intent(SRC_API_COMM);
			sendBroadcast(intent, RECEIVE_API_COMM_NOTIFICATIONS);
			ApiComm apiComm = new ApiComm();
			if (apiComm.getConnectivityTimeout() > 0) {
				try {
					Thread.sleep(apiComm.getConnectivityTimeout()*1000);
					if (!app.airplaneMode.isEnabled(app.getApplicationContext())) {
						if (!app.apiComm.isTransmitting) {
							Log.d(TAG, "Connectivity timeout reached. Entering Airplane Mode.");
							apiComm.resetTransmissionState();
							app.airplaneMode.setOn(app.getApplicationContext());
						} else {
							Log.d(TAG, "Connectivity timeout reached, but transmission is in progress. Delaying timeout.");
							Thread.sleep(60*1000);
							if (!app.airplaneMode.isEnabled(app.getApplicationContext())) {
								Log.d(TAG, "2nd timeout reached. Entering Airplane Mode.");
								apiComm.resetTransmissionState();
								app.airplaneMode.setOn(app.getApplicationContext());
							}
						}
					}
				} catch (InterruptedException e) {
				}
			}
		} else {
			if (app.verboseLogging) Log.d(TAG, app.isServiceRunning_ApiComm ? "Skipping (off hours)" : "Skipping (first run)");
			if (timeOfDay.isDataGenerationEnabled(context)) {
				app.airplaneMode.setOn(app.getApplicationContext());
			}
			app.isServiceRunning_ApiComm = true;
		}
	}
}
