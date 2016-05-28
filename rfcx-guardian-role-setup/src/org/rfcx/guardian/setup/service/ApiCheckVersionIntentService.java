package org.rfcx.guardian.setup.service;

import org.rfcx.guardian.setup.RfcxGuardian;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ApiCheckVersionIntentService extends IntentService {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiCheckVersionIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "ApiCheckVersionIntentService";
		
	public ApiCheckVersionIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		int prefsInstallOfflineToggleThreshold = app.rfcxPrefs.getPrefAsInt("install_offline_toggle_threshold");
		
		if (app.deviceConnectivity.isConnected()) {
			app.rfcxServiceHandler.triggerService("ApiCheckVersion", true);
		} else if (	(app.deviceConnectivity.lastDisconnectedAt() > app.deviceConnectivity.lastConnectedAt())
				&& 	((app.deviceConnectivity.lastDisconnectedAt()-app.deviceConnectivity.lastConnectedAt()) > prefsInstallOfflineToggleThreshold)
				) {
			Log.e(TAG, "Disconnected for more than " + Math.round( prefsInstallOfflineToggleThreshold / ( 60 * 1000 ) ) + " minutes.");
			// nothing happens here
			// in order to ensure no conflict with other apps running in parallel
		} else {
			Log.d(TAG,"Disconnected for less than " + Math.round( prefsInstallOfflineToggleThreshold / ( 60 * 1000 ) ) + " minutes.");
		}
	}

}
