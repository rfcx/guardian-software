package org.rfcx.guardian.admin.device.android.control;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ScheduledSntpSyncService extends IntentService {

	private static final String SERVICE_NAME = "ScheduledSntpSync";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScheduledSntpSyncService");
	
	public static final long SCHEDULED_SNTP_SYNC_CYCLE_DURATION = 20 * ( 60 * 1000 ); // every 20 minutes
		
	public ScheduledSntpSyncService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxServiceHandler.triggerService("SntpSyncJob", true);
	
	}
	
	
}
