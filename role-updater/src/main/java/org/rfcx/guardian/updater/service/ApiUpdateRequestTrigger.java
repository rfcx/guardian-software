package org.rfcx.guardian.updater.service;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

import android.app.IntentService;
import android.content.Intent;

public class ApiUpdateRequestTrigger extends IntentService {

	public static final String SERVICE_NAME = "ApiUpdateRequestTrigger";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiUpdateRequestTrigger");
		
	public ApiUpdateRequestTrigger() {
		super(logTag);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

		app.apiUpdateRequestUtils.attemptToTriggerUpdateRequest(false, true);

	}

}
