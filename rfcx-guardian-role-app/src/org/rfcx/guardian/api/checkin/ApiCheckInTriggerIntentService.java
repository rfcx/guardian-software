package org.rfcx.guardian.api.checkin;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.audio.RfcxAudio;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class ApiCheckInTriggerIntentService extends IntentService {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ApiCheckInTriggerIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "ApiCheckInTrigger";
		
	public ApiCheckInTriggerIntentService() {
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
				
//				ContentValues queueValues = new ContentValues();
//				queueValues.put("created_at", encodedAudio[0]);
//				queueValues.put("timestamp", encodedAudio[1]);
//				queueValues.put("format", encodedAudio[2]);
//				queueValues.put("digest", encodedAudio[3]);
//				queueValues.put("samplerate", encodedAudio[4]);
//				queueValues.put("bitrate", encodedAudio[5]);
//				queueValues.put("codec", encodedAudio[6]);
//				queueValues.put("duration", encodedAudio[7]);
//				queueValues.put("encode_duration", encodedAudio[8]);
//				queueValues.put("cbr_or_vbr", RfcxAudio.isEncodedWithVbr(encodedAudio[6]) ? "vbr" : "cbr");
//				queueValues.put("filepath", encodedAudio[9]);
//	
//				Uri queuedCheckInUri = getContentResolver().insert( Uri.parse(RfcxRole.ContentProvider.api.URI_CHECKIN), queueValues );
//			
//				if (queuedCheckInUri == null) { Log.e(logTag,"Failed to trigger CheckIn via ContentProvider..."); }
				
				String[] audioInfo = new String[] {
						encodedAudio[0], //values.getAsString("created_at"),
						encodedAudio[1], //values.getAsString("timestamp"),
						encodedAudio[2], //values.getAsString("format"),
						encodedAudio[3], //values.getAsString("digest"),
						encodedAudio[4], //values.getAsString("samplerate"),
						encodedAudio[5], //values.getAsString("bitrate"),
						encodedAudio[6], //values.getAsString("codec"),
						(RfcxAudio.isEncodedWithVbr(encodedAudio[6]) ? "vbr" : "cbr"), //values.getAsString("cbr_or_vbr"),
						encodedAudio[8] //values.getAsString("encode_duration")
				};
				
				if (app.apiCheckInUtils.addCheckInToQueue(audioInfo, encodedAudio[9])) {
					Log.d(logTag, "Check In Queued: "+encodedAudio[9]);
				}
				
				// Wait an extra second between cycles
				// This is probably not necessary... 
				// but it's also likely harmless.
				Thread.sleep(1000);
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
			
		}
	
	}

}
