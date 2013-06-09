package org.rfcx.src_api;

import org.rfcx.src_android.RfcxSource;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ApiCommService extends IntentService {

	private static final String TAG = ApiCommService.class.getSimpleName();
	
	public static final String SRC_API_COMM = "org.rfcx.src_android.SRC_API_COMM";
	public static final String RECEIVE_API_COMM_NOTIFICATIONS = "org.rfcx.src_android.RECEIVE_API_COMM_NOTIFICATIONS";
	
	public ApiCommService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxSource rfcxSource = (RfcxSource) getApplication();
		if (rfcxSource.isServiceRunning_ApiComm) {
			rfcxSource.airplaneMode.setOff(rfcxSource.getApplicationContext());	
			Intent intent = new Intent(SRC_API_COMM);
			sendBroadcast(intent, RECEIVE_API_COMM_NOTIFICATIONS);
			ApiComm apiComm = new ApiComm();
			if (apiComm.getConnectivityTimeout() > 0) {
				try {
					Thread.sleep(apiComm.getConnectivityTimeout()*1000);
					if (!rfcxSource.airplaneMode.isEnabled(rfcxSource.getApplicationContext())) {
						if (!rfcxSource.apiComm.isTransmitting) {
							Log.d(TAG, "Connectivity timeout reached. Entering Airplane Mode.");
							rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
						} else {
							Log.d(TAG, "Connectivity timeout reached, but transmission is in progress. Delaying timeout.");
							Thread.sleep(60*1000);
							if (!rfcxSource.airplaneMode.isEnabled(rfcxSource.getApplicationContext())) {
								Log.d(TAG, "2nd timeout reached. Entering Airplane Mode.");
								rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
							}
						}
					}
				} catch (InterruptedException e) {
				}
			}
		} else {
			Log.d(TAG, "Skipping (first run)");
			rfcxSource.isServiceRunning_ApiComm = true;
		}
	}
}
