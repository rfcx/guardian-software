package org.rfcx.guardian.admin.status;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class StatusCacheService extends IntentService {

	public static final String SERVICE_NAME = "StatusCache";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "StatusCacheService");

	public StatusCacheService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxSvc.reportAsActive(SERVICE_NAME);
		
		try {

			app.rfcxStatus.forceUpdatesForAllGroups(false);
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			app.rfcxSvc.stopService(SERVICE_NAME, false);
		}
		
	}
	
	
}
