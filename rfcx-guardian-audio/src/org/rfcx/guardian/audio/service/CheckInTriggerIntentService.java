package org.rfcx.guardian.audio.service;

import java.util.List;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class CheckInTriggerIntentService extends IntentService {
	
	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+CheckInTriggerIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".CHECKIN_TRIGGER";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".RECEIVE_CHECKIN_TRIGGER_NOTIFICATIONS";
	
	public CheckInTriggerIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		String [] lastAudio = app.audioDb.dbEncoded.getLatestRow();
		ContentValues lastAudioValues = new ContentValues();
		lastAudioValues.put("created_at", lastAudio[0]);
		lastAudioValues.put("timestamp", lastAudio[1]);
		lastAudioValues.put("format", lastAudio[2]);
		lastAudioValues.put("digest", lastAudio[3]);
		lastAudioValues.put("filepath", app.audioEncode.getAudioFileLocation_Complete_PostZip((long) Long.parseLong(lastAudio[1]), lastAudio[2]));
		
		Uri createdCheckInUri = getContentResolver().insert(
				Uri.parse(RfcxConstants.RfcxContentProvider.api.URI_1),
				lastAudioValues);
		if (createdCheckInUri == null) {
			Log.e(TAG,"Error triggering CheckIn via ContentProvider in 'api' role...");
		}
	
	}

}