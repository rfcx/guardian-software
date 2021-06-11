package org.rfcx.guardian.guardian.api.methods.ping;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class ScheduledApiPingService extends IntentService {

	public static final String SERVICE_NAME = "ScheduledApiPing";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScheduledApiPingService");

	public ScheduledApiPingService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxSvc.reportAsActive(SERVICE_NAME);

		String[] includePingFields = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PING_CYCLE_FIELDS).split(",");

		if (app.apiPingUtils.isScheduledPingAllowedAtThisTimeOfDay()) {
			app.apiPingUtils.sendPing(
					ArrayUtils.doesStringArrayContainString(includePingFields, "all"),
					includePingFields,
					ArrayUtils.doesStringArrayContainString(includePingFields, "meta") ? 1 : 0,
					"all",
					app.apiPingUtils.hasScheduledPingAlreadyRun
			);
		} else {

			Log.e(logTag, "Scheduled Ping blocked due to time of day.");
		}

		app.apiPingUtils.hasScheduledPingAlreadyRun = true;
		
	}
	
	
}
