package org.rfcx.guardian.guardian.api.methods.checkin;

import org.rfcx.guardian.utility.asset.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;

import org.rfcx.guardian.guardian.RfcxGuardian;

public class ApiCheckInQueueService extends IntentService {

	private static final String SERVICE_NAME = "ApiCheckInQueue";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInQueueService");
		
	public ApiCheckInQueueService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
		
		try {
				
			for (String[] encodedAudio : app.audioEncodeDb.dbEncoded.getAllRows()) {

				if (encodedAudio[9].equalsIgnoreCase("stream")) {

					app.apiCheckInUtils.addCheckInToQueue(
						new String[]{
								encodedAudio[0], // created_at
								encodedAudio[1], //	timestamp
								encodedAudio[2], //	format
								encodedAudio[3], //	digest
								encodedAudio[4], //	sample rate
								encodedAudio[5], //	bitrate
								encodedAudio[6], //	codec
								(RfcxAudioUtils.isEncodedWithVbr(encodedAudio[6]) ? "vbr" : "cbr"), //	cbr or vbr
								encodedAudio[8], // encode duration
								"16bit",         // sample precision, in bits
								encodedAudio[7] // capture duration
						}, encodedAudio[10]);
				}
			}


			if (!app.apiCheckInHealthUtils.isApiCheckInDisabled(true)) {
				app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("ApiCheckInJob", 3 * app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000 );
				app.apiMqttUtils.updateFailedCheckInThresholds();
			}

			// if the queued table has grown beyond the maximum threshold, stash the oldest checkins 
			app.apiCheckInUtils.stashOrArchiveOldestCheckIns();
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME);
		}
		
	}
	
	
}
