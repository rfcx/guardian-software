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
		Log.d(TAG, "ApiCommIntentService started");
		RfcxSource rfcxSource = (RfcxSource) getApplication();

		rfcxSource.airplaneMode.setOff(rfcxSource.getApplicationContext());
		
		Intent intent = new Intent(SRC_API_COMM);
		sendBroadcast(intent, RECEIVE_API_COMM_NOTIFICATIONS);
		
		if (ApiComm.CONNECTIVITY_TIMEOUT > 0) {
			try {
				Thread.sleep(ApiComm.CONNECTIVITY_TIMEOUT*1000);
				if (!rfcxSource.airplaneMode.isEnabled(rfcxSource.getApplicationContext())) {
					Log.d(TAG, "Connectivity timeout/duration limit reached. Entering Airplane Mode.");
					rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());
				}
			} catch (InterruptedException e) {
			}
		}
	}

}
