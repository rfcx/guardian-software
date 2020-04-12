package org.rfcx.guardian.admin;


import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;

public class ServiceMonitor extends IntentService {

	private static final String SERVICE_NAME = "ServiceMonitor";
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ServiceMonitor.class.getSimpleName());
	
	public static final long SERVICE_MONITOR_CYCLE_DURATION = 600000;
	// Please note that services that register as 'active' less frequently than this cycle duration will be forced to retrigger.
	// For continuous, long running services, measures should be taken to ensure that they register as 'active' more often than this monitor runs.
		
	public ServiceMonitor() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.rfcxServiceHandler.isRunning(SERVICE_NAME)) {
			
			app.rfcxServiceHandler.triggerServiceSequence( "ServiceMonitorSequence", app.RfcxCoreServices, false, SERVICE_MONITOR_CYCLE_DURATION );
		}
		
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
	}
	
	
}
