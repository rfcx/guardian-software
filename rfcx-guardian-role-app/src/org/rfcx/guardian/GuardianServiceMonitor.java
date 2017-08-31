package org.rfcx.guardian;


import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;

public class GuardianServiceMonitor extends IntentService {
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+GuardianServiceMonitor.class.getSimpleName();
	
	private static final String SERVICE_NAME = "ServiceMonitor";
		
	public GuardianServiceMonitor() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.rfcxServiceHandler.isRunning(SERVICE_NAME)) {
			
			app.rfcxServiceHandler.triggerServiceSequence( "ServiceMonitorSequence", app.RfcxCoreServices, false );
		}
		
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
	}
	
	
}
