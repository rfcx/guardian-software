package org.rfcx.guardian.intentservice;

import java.util.List;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class AudioEncode extends IntentService {
	
	private static final String TAG = "RfcxGuardian-"+AudioEncode.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian.AUDIO_ENCODE";
	public static final String NULL_EXC = "org.rfcx.guardian.RECEIVE_AUDIO_ENCODE_NOTIFICATIONS";
	
	public AudioEncode() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NULL_EXC);
		
		List<String[]> capturedRows = app.audioDb.dbCaptured.getAllCaptured();
		Log.v(TAG, "Running AudioEncodeIntentService... "+capturedRows.size()+" file(s) to encode.");
		for (String[] capturedRow : capturedRows) {
			Log.i(TAG, "Encoding: '"+capturedRow[0]+"','"+capturedRow[1]+"','"+capturedRow[2]+"'");
			if (capturedRow[2].equals("wav")) {
				app.audioCore.encodeCaptureAudio(capturedRow[1], "flac", capturedRow[0], app.audioDb);
				app.apiCore.createCheckIn();
			} else {
				app.audioDb.dbCaptured.clearCapturedBefore(app.audioDb.dateTimeUtils.getDateFromString(capturedRow[0]));
				String digest = (new FileUtils()).sha1Hash(app.audioCore.wavDir.substring(0,app.audioCore.wavDir.lastIndexOf("/"))+"/"+capturedRow[2]+"/"+capturedRow[1]+"."+capturedRow[2]);
				app.audioDb.dbEncoded.insert(capturedRow[1], capturedRow[2],digest);
				app.apiCore.createCheckIn();
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			}
		}
	
	}

}
