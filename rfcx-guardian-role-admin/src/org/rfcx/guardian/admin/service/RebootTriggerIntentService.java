package org.rfcx.guardian.admin.service;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;

public class RebootTriggerIntentService extends IntentService {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, RebootTriggerIntentService.class);
	
	private static final String SERVICE_NAME = "RebootTrigger";
	
	public RebootTriggerIntentService() {
		super(logTag);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		
		ShellCommands.executeCommand(
//				"am broadcast android.intent.action.ACTION_SHUTDOWN"
//				+" && sleep 5"
//				+" && reboot", 
				"am start -a android.intent.action.REBOOT",
				null, true, ((RfcxGuardian) getApplication()).getApplicationContext());
		
	}

}
