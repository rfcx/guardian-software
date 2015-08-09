package org.rfcx.guardian.audio.service;

import java.util.List;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class AudioEncodeIntentService extends IntentService {
	
	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+AudioEncodeIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".AUDIO_ENCODE";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".RECEIVE_AUDIO_ENCODE_NOTIFICATIONS";
	
	public AudioEncodeIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		List<String[]> capturedRows = app.audioDb.dbCaptured.getAllCaptured();
		for (String[] capturedRow : capturedRows) {
			Log.i(TAG, "Encoding: '"+capturedRow[0]+"','"+capturedRow[1]+"','"+capturedRow[2]+"'");
			if (capturedRow[2].equals("wav")) {
				app.audioCapture.encodeCaptureAudio(capturedRow[1], "flac", capturedRow[0], app.audioDb);
				//make sure the previous step(s) are synchronous or else the checkin will occur before the encode...
				app.audioEncode.triggerCheckInAfterEncode(app.getApplicationContext());
			} else {
				app.audioDb.dbCaptured.clearCapturedBefore(app.audioDb.dateTimeUtils.getDateFromString(capturedRow[0]));
				String digest = (new FileUtils()).sha1Hash(app.audioCapture.wavDir.substring(0,app.audioCapture.wavDir.lastIndexOf("/"))+"/"+capturedRow[2]+"/"+capturedRow[1]+"."+capturedRow[2]);
				app.audioDb.dbEncoded.insert(capturedRow[1], capturedRow[2],digest);
				//make sure the previous step(s) are synchronous or else the checkin will occur before the encode...
				app.audioEncode.triggerCheckInAfterEncode(app.getApplicationContext());
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}
	
	}

}
