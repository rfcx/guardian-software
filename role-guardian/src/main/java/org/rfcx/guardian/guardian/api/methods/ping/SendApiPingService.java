package org.rfcx.guardian.guardian.api.methods.ping;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class SendApiPingService extends IntentService {

	public static final String SERVICE_NAME = "SendApiPing";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SendApiPingService");

	public SendApiPingService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxSvc.reportAsActive(SERVICE_NAME);

		app.apiPingUtils.sendPing(true, new String[]{}, true );
		
	}
	
	
}
