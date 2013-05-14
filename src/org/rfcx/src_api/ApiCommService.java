package org.rfcx.src_api;

import org.rfcx.src_android.RfcxSource;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ApiCommService extends IntentService {

	private static final String TAG = ApiCommService.class.getSimpleName();
	
	public static final String SRC_API_COMM = "org.rfcx.src_android.SRC_API_COMM";
//	public static final String SRC_API_COMM_EXTRA_COUNT = "SRC_API_COMM_EXTRA_COUNT";
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
//		intent.putExtra(SRC_API_COMM_EXTRA_COUNT, 1);
		sendBroadcast(intent, RECEIVE_API_COMM_NOTIFICATIONS);
		
//		Thread.sleep(ApiComm.CONNECTIVITY_TIMEOUT*1000);
//		if (!rfcxSource.airplaneMode.isEnabled(rfcxSource.getApplicationContext())) rfcxSource.airplaneMode.setOn(rfcxSource.getApplicationContext());

	}

}
