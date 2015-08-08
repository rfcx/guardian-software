package org.rfcx.guardian.updater.service;

import org.rfcx.guardian.updater.R;
import org.rfcx.guardian.updater.RfcxGuardian;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ApiCheckVersionIntentService extends IntentService {

	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.Constants.ROLE_NAME+"-"+ApiCheckVersionIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+org.rfcx.guardian.utility.Constants.ROLE_NAME.toLowerCase()+".INSTALLER_SERVICE";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+org.rfcx.guardian.utility.Constants.ROLE_NAME.toLowerCase()+".RECEIVE_INSTALLER_SERVICE_NOTIFICATIONS";

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
