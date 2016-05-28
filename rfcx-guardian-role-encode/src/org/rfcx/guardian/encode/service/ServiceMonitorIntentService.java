package org.rfcx.guardian.encode.service;


import java.util.Locale;

import org.rfcx.guardian.encode.RfcxGuardian;

import android.app.IntentService;
import android.content.Intent;

public class ServiceMonitorIntentService extends IntentService {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ServiceMonitorIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "ServiceMonitor";
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase(Locale.US)+".SERVICE_MONITOR";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase(Locale.US)+".RECEIVE_SERVICE_MONITOR_NOTIFICATIONS";
	
	public ServiceMonitorIntentService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
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
