package org.rfcx.guardian.system.service;


import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceMonitorIntentService extends IntentService {
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ServiceMonitorIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".SERVICE_MONITOR";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".RECEIVE_SERVICE_MONITOR_NOTIFICATIONS";
	
	public ServiceMonitorIntentService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.rfcxServiceHandler.isRunning("ServiceMonitor")) {
			
			Log.v(TAG, "Running Service Monitor...");
			
			app.rfcxServiceHandler.triggerService(new String[] { "DeviceState" }, false);
			app.rfcxServiceHandler.triggerService(new String[] { "DeviceSensor" }, false);
			
		} else {
			// the Monitor logic won't run the first time the intent service is fired
			app.rfcxServiceHandler.setRunState("ServiceMonitor", true);
			
		}
	}
	
	
}
