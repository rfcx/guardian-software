package org.rfcx.guardian.guardian.api;

import android.app.IntentService;
import android.content.Intent;

import org.json.JSONException;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ApiCheckInMetaSnapshotService extends IntentService {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInMetaSnapshotService.class);

	private static final String SERVICE_NAME = "ApiCheckInMetaSnapshot";

	public ApiCheckInMetaSnapshotService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

		try {

			app.apiCheckInUtils.createSystemMetaDataJsonSnapshot();

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}

	}
	
	
}
