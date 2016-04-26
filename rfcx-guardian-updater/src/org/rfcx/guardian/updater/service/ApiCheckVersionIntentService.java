package org.rfcx.guardian.updater.service;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ApiCheckVersionIntentService extends IntentService {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiCheckVersionIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".INSTALLER_SERVICE";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".RECEIVE_INSTALLER_SERVICE_NOTIFICATIONS";
	
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
				&& 	((app.lastDisconnectedAt-app.lastConnectedAt) > app.INSTALL_OFFLINE_TOGGLE_THRESHOLD)
				) {
			Log.e(TAG, "Disconnected for more than " + Math.round( app.INSTALL_OFFLINE_TOGGLE_THRESHOLD / ( 60 * 1000 ) ) + " minutes.");
			// nothing happens here
			// in order to ensure no conflict with other apps running in parallel
		} else {
			Log.d(TAG,"Disconnected for less than " + Math.round( app.INSTALL_OFFLINE_TOGGLE_THRESHOLD / ( 60 * 1000 ) ) + " minutes.");
		}
	}

}
