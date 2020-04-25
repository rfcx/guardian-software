package org.rfcx.guardian.updater.service;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;

public class ApiCheckVersionTrigger extends IntentService {

	private static final String SERVICE_NAME = "ApiCheckVersionTrigger";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckVersionTrigger");
		
	public ApiCheckVersionTrigger() {
		super(logTag);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

		app.apiCheckVersionUtils.attemptToTriggerCheckIn();

	}

}
