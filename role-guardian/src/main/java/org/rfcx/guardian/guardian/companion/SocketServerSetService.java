package org.rfcx.guardian.guardian.companion;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class SocketServerSetService extends IntentService {

	public static final String SERVICE_NAME = "SocketServerSet";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SocketServerSetService");

	public SocketServerSetService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

		try {

			if (app.apiSocketUtils.isSocketServerEnabled(true)) {

				app.apiSocketUtils.startServer();
				app.rfcxSvc.triggerService( CompanionCommunicationService.SERVICE_NAME, true);

			} else {

				app.rfcxSvc.stopService( CompanionCommunicationService.SERVICE_NAME);
				app.apiSocketUtils.stopServer();
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

	}
	
	
}
