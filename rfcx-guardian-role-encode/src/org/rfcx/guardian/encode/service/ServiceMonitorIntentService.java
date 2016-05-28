package org.rfcx.guardian.encode.service;


import org.rfcx.guardian.encode.RfcxGuardian;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;

public class ServiceMonitorIntentService extends IntentService {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ServiceMonitorIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "ServiceMonitor";
		
	public ServiceMonitorIntentService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.rfcxServiceHandler.isRunning(SERVICE_NAME)) {
			
			app.rfcxServiceHandler.triggerServiceSequence(
					"ServiceMonitorSequence", 
						new String[] { 
							"AudioEncodeTrigger"
						}, 
					false);
		}
		
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
	}
	
	
}
