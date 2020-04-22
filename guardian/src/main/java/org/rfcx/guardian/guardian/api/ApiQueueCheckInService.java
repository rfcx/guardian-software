package org.rfcx.guardian.guardian.api;

import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class ApiQueueCheckInService extends IntentService {

	private static final String SERVICE_NAME = "ApiQueueCheckIn";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiQueueCheckInService");
		
	public ApiQueueCheckInService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
		
		try {
				
			for (String[] encodedAudio : app.audioEncodeDb.dbEncoded.getRowsWithLimit(10)) {
				
				app.apiCheckInUtils.addCheckInToQueue(
						new String[] {
								encodedAudio[0], // created_at
								encodedAudio[1], //	timestamp
								encodedAudio[2], //	format
								encodedAudio[3], //	digest
								encodedAudio[4], //	sample rate
								encodedAudio[5], //	bitrate
								encodedAudio[6], //	codec
								(RfcxAudioUtils.isEncodedWithVbr(encodedAudio[6]) ? "vbr" : "cbr"), //	cbr or vbr
								encodedAudio[8], // encode duration
								"16bit" 		 // sample precision, in bits
						}, encodedAudio[9]);

				app.diagnosticUtils.updateRecordedDiagnostic();
			}


			if (!app.rfcxPrefs.getPrefAsBoolean("enable_checkin_publish")) { 
				Log.v(logTag, "No CheckIn triggered because org.rfcx.guardian.guardian is in offline mode.");
			} else {
				app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("ApiCheckInJob", 3 * app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000 );
				app.apiCheckInUtils.updateFailedCheckInThresholds();
			}

			// if the queued table has grown beyond the maximum threshold, stash the oldest checkins 
			app.apiCheckInUtils.stashOldestCheckIns();
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME);
		}
		
	}
	
	
}
