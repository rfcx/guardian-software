package org.rfcx.guardian.setup.service;

import java.util.Locale;

import org.rfcx.guardian.setup.RfcxGuardian;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ApiCheckVersionIntentService extends IntentService {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiCheckVersionIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "ApiCheckVersionIntentService";
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase(Locale.US)+".INSTALLER_SERVICE";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase(Locale.US)+".RECEIVE_INSTALLER_SERVICE_NOTIFICATIONS";
	
	public ApiCheckVersionIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		int INSTALL_OFFLINE_TOGGLE_THRESHOLD = app.rfcxPrefs.getPrefAsInt("install_offline_toggle_threshold");
		
		if (app.deviceConnectivity.isConnected()) {
			app.rfcxServiceHandler.triggerService("ApiCheckVersion", true);
		} else if (	(app.deviceConnectivity.lastDisconnectedAt() > app.deviceConnectivity.lastConnectedAt())
				&& 	((app.deviceConnectivity.lastDisconnectedAt()-app.deviceConnectivity.lastConnectedAt()) > INSTALL_OFFLINE_TOGGLE_THRESHOLD)
				) {
			Log.e(TAG, "Disconnected for more than " + Math.round( INSTALL_OFFLINE_TOGGLE_THRESHOLD / ( 60 * 1000 ) ) + " minutes.");
			// nothing happens here
			// in order to ensure no conflict with other apps running in parallel
		} else {
			Log.d(TAG,"Disconnected for less than " + Math.round( INSTALL_OFFLINE_TOGGLE_THRESHOLD / ( 60 * 1000 ) ) + " minutes.");
		}
	}

}
