package org.rfcx.guardian.admin.asset;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ScheduledAssetCleanupService extends IntentService {

	public static final String SERVICE_NAME = "ScheduledAssetCleanup";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScheduledAssetCleanupService");

	public static final int ASSET_CLEANUP_CYCLE_DURATION_MINUTES = 120;

	public ScheduledAssetCleanupService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

		try {
			app.assetUtils.runFileSystemAssetCleanup();
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		
	}
	
	
}
