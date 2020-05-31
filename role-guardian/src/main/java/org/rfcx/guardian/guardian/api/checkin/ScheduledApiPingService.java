package org.rfcx.guardian.guardian.api.checkin;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ScheduledApiPingService extends IntentService {

	private static final String SERVICE_NAME = "ScheduledApiPing";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScheduledApiPingService");

	public static final long SCHEDULED_API_PING_CYCLE_DURATION = 30 * ( 60 * 1000 ); // every 30 minutes

	public ScheduledApiPingService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

		app.apiCheckInUtils.sendMqttPing(true, new String[]{} );
		
	}
	
	
}
