package org.rfcx.guardian.installer.service;

import org.rfcx.guardian.installer.R;
import org.rfcx.guardian.installer.RfcxGuardian;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ApiCheckVersionIntentService extends IntentService {

	private static final String TAG = "Rfcx-"+R.string.log_name+"-"+ApiCheckVersionIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.installer.INSTALLER_SERVICE";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.installer.RECEIVE_INSTALLER_SERVICE_NOTIFICATIONS";

	private final long toggleAirplaneModeIfDisconnectedForLongerThan = 15;
	
	public ApiCheckVersionIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.isConnected) {
			app.triggerService("ApiCheckVersion", true);
		} else if (	(app.lastDisconnectedAt > app.lastConnectedAt)
				&& 	((app.lastDisconnectedAt-app.lastConnectedAt) > (toggleAirplaneModeIfDisconnectedForLongerThan*60*1000))
				) {
			Log.e(TAG, "Disconnected for more than "+toggleAirplaneModeIfDisconnectedForLongerThan+" minutes.");
			// nothing happens here
			// in order to ensure no conflict with other apps running in parallel
		} else {
			Log.d(TAG,"Disconnected for less than "+toggleAirplaneModeIfDisconnectedForLongerThan+" minutes.");
		}
	}

}
