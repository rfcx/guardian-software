package org.rfcx.guardian.admin.device.android.control;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.IntentService;
import android.content.Intent;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ScheduledRebootService extends IntentService {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ScheduledRebootService.class);
	
	private static final String SERVICE_NAME = "ScheduledReboot";
		
	public ScheduledRebootService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxServiceHandler.triggerService("RebootTrigger", true);
	
	}
	
	
}
