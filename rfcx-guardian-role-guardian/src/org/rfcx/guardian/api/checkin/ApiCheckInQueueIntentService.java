package org.rfcx.guardian.api.checkin;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ApiCheckInQueueIntentService extends IntentService {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInQueueIntentService.class);
	
	private static final String SERVICE_NAME = "ApiCheckInQueue";
		
	public ApiCheckInQueueIntentService() {
		super(logTag);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));

		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		for (String[] encodedAudio : app.audioEncodeDb.dbEncoded.getAllRows()) {
			
			try {
				
				app.audioEncodeDb.dbEncoded.deleteSingleRow(encodedAudio[1]);
				
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
				
				boolean addCheckInToQueue = app.apiCheckInUtils.addCheckInToQueue(audioInfo, encodedAudio[9]);
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
			
		}
	
	}

}
