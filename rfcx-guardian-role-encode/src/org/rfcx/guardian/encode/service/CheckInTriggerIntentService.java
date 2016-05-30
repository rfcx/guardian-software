package org.rfcx.guardian.encode.service;

import org.rfcx.guardian.encode.RfcxGuardian;
import org.rfcx.guardian.utility.audio.RfcxAudio;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class CheckInTriggerIntentService extends IntentService {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+CheckInTriggerIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "CheckInTrigger";
		
	public CheckInTriggerIntentService() {
		super(logTag);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));

		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		for (String[] encodedAudio : app.audioEncodeDb.dbEncoded.getAllRows()) {
			
			try {
				
				ContentValues queueValues = new ContentValues();
				queueValues.put("created_at", encodedAudio[0]);
				queueValues.put("timestamp", encodedAudio[1]);
				queueValues.put("format", encodedAudio[2]);
				queueValues.put("digest", encodedAudio[3]);
				queueValues.put("samplerate", encodedAudio[4]);
				queueValues.put("bitrate", encodedAudio[5]);
				queueValues.put("codec", encodedAudio[6]);
				queueValues.put("duration", encodedAudio[7]);
				queueValues.put("encode_duration", encodedAudio[8]);
				queueValues.put("cbr_or_vbr", RfcxAudio.isEncodedWithVbr(encodedAudio[6]) ? "vbr" : "cbr");
				queueValues.put("filepath", encodedAudio[9]);
	
				Uri queuedCheckInUri = getContentResolver().insert( Uri.parse(RfcxRole.ContentProvider.api.URI_CHECKIN), queueValues );
			
				if (queuedCheckInUri == null) { Log.e(logTag,"Failed to trigger CheckIn via ContentProvider..."); }
				
				// Wait an extra second between cycles in case the 'api' role needs a pause.
				// This is probably not necessary... but it's also likely harmless.
				Thread.sleep(1000);
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
			
		}
	
	}

}
