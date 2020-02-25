package org.rfcx.guardian.setup.service;

import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.setup.RfcxGuardian;

public class ApiCheckVersionIntentService extends IntentService {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckVersionIntentService.class);
	
	private static final String SERVICE_NAME = "ApiCheckVersionIntentService";
		
	public ApiCheckVersionIntentService() {
		super(logTag);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		long offlineDurationToggleThreshold = 15 * 60 * 1000; // 15 minutes
		
		if (app.deviceConnectivity.isConnected()) {
			
			app.rfcxServiceHandler.triggerService("ApiCheckVersion", true);
			
		} else if (	
				(app.deviceConnectivity.lastDisconnectedAt() > app.deviceConnectivity.lastConnectedAt())
			&& 	( (app.deviceConnectivity.lastDisconnectedAt() - app.deviceConnectivity.lastConnectedAt() ) > offlineDurationToggleThreshold )
			) {
			
			Log.e(logTag, "Disconnected for more than " + Math.round( offlineDurationToggleThreshold / ( 60 * 1000 ) ) + " minutes.");
			// nothing happens here in order to ensure no conflict with other apps running in parallel
			
		} else {
			
			Log.d(logTag,"Disconnected for less than " + Math.round( offlineDurationToggleThreshold / ( 60 * 1000 ) ) + " minutes.");
		}
	}

}
