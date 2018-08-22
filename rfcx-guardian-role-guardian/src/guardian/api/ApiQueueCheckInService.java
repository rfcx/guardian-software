package guardian.api;


import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import guardian.RfcxGuardian;

public class ApiQueueCheckInService extends IntentService {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiQueueCheckInService.class);
	
	private static final String SERVICE_NAME = "ApiQueueCheckIn";
		
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
				
				String[] audioInfo = new String[] {
						encodedAudio[0], // "created_at"
						encodedAudio[1], //"timestamp"
						encodedAudio[2], //"format"
						encodedAudio[3], //"digest"
						encodedAudio[4], //"samplerate"
						encodedAudio[5], //"bitrate"
						encodedAudio[6], //"codec"
						(RfcxAudioUtils.isEncodedWithVbr(encodedAudio[6]) ? "vbr" : "cbr"), //"cbr_or_vbr"
						encodedAudio[8] //"encode_duration"
					};
				
				app.apiCheckInUtils.addCheckInToQueue(audioInfo, encodedAudio[9]);
	
			}


			if (app.rfcxPrefs.getPrefAsBoolean("checkin_offline_mode")) { 
				Log.v(logTag, "No checkin triggered because guardian is in offline mode.");
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
