package org.rfcx.guardian.audio;

import org.rfcx.guardian.RfcxGuardian;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class AudioEncodeIntentService extends IntentService {
	
	private static final String TAG = AudioEncodeIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.GUARDIAN_AUDIO_ENCODE";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian.RECEIVE_AUDIO_ENCODE_NOTIFICATIONS";
	
	public AudioEncodeIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		if (app.verboseLogging) Log.d(TAG, "Running AudioEncodeIntentService...");
		
		for (String[] capturedRow : app.audioDb.dbCaptured.getAllCaptured()) {
			Log.d(TAG, "'"+capturedRow[0]+"' * '"+capturedRow[1]+"' * '"+capturedRow[2]+"'");
			if (capturedRow[2].equals("wav")) {
				app.audioCore.encodeCaptureAudio(capturedRow[1], "flac", capturedRow[0], app.audioDb);
			} else {
				Log.d(TAG, "Captured file does not need to be re-encoded...");
				app.audioDb.dbCaptured.clearCapturedBefore(app.audioDb.dateTimeUtils.getDateFromString(capturedRow[0]));
				app.audioDb.dbEncoded.insert(capturedRow[1], capturedRow[2]);
			}
		}
	}

}
