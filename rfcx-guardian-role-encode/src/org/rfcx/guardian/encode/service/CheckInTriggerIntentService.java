package org.rfcx.guardian.encode.service;

import java.io.File;

import org.rfcx.guardian.encode.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class CheckInTriggerIntentService extends IntentService {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+CheckInTriggerIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "CheckInTrigger";
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".CHECKIN_TRIGGER";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".RECEIVE_CHECKIN_TRIGGER_NOTIFICATIONS";
	
	public CheckInTriggerIntentService() {
		super(logTag);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);

		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		for (String[] encodedAudio : app.audioEncodeDb.dbEncoded.getAllRows()) {
			
			ContentValues lastAudioValues = new ContentValues();
			lastAudioValues.put("created_at", encodedAudio[0]);
			lastAudioValues.put("timestamp", encodedAudio[1]);
			lastAudioValues.put("format", encodedAudio[2]);
			lastAudioValues.put("digest", encodedAudio[3]);
			lastAudioValues.put("samplerate", encodedAudio[4]);
			lastAudioValues.put("bitrate", encodedAudio[5]);
			lastAudioValues.put("codec", encodedAudio[6]);
			lastAudioValues.put("duration", encodedAudio[7]);
			lastAudioValues.put("encode_duration", encodedAudio[8]);
			lastAudioValues.put("cbr_or_vbr", "cbr");
			lastAudioValues.put("filepath", encodedAudio[9]);

			Uri queuedCheckInUri = getContentResolver().insert( Uri.parse(RfcxRole.ContentProvider.api.URI_CHECKIN), lastAudioValues );
			
			if (queuedCheckInUri == null) {
				Log.e(logTag,"Failed to trigger CheckIn via ContentProvider...");
				if ((new File(encodedAudio[9])).exists()) { (new File(encodedAudio[9])).delete(); }
			}
			
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
			
		}
	
	}

}
