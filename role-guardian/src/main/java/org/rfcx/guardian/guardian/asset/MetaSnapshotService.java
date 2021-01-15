package org.rfcx.guardian.guardian.asset;

import android.app.IntentService;
import android.content.Intent;

import org.json.JSONException;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class MetaSnapshotService extends IntentService {

	public static final String SERVICE_NAME = "MetaSnapshot";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "MetaSnapshotService");

	public MetaSnapshotService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

		try {

			app.apiCheckInJsonUtils.createSystemMetaDataJsonSnapshot();

		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}

	}
	
	
}
