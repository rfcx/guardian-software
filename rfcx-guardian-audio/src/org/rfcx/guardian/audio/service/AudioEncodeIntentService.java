package org.rfcx.guardian.audio.service;

import java.io.File;
import java.util.List;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.GZipUtils;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class AudioEncodeIntentService extends IntentService {
	
	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+AudioEncodeIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".AUDIO_ENCODE";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxConstants.ROLE_NAME.toLowerCase()+".RECEIVE_AUDIO_ENCODE_NOTIFICATIONS";

    private FileUtils fileUtils = new FileUtils();
    private GZipUtils gZipUtils = new GZipUtils();
    private DateTimeUtils dateTimeUtils = new DateTimeUtils();
    
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
			
			File preEncodeFile = new File(app.audioEncode.getAudioFileLocation_PreEncode((long) Long.parseLong(capturedRow[1]),capturedRow[2]));
			File postEncodeFile = new File(app.audioEncode.getAudioFileLocation_PostEncode((long) Long.parseLong(capturedRow[1]),capturedRow[2]));
			File gZippedFile = new File(app.audioEncode.getAudioFileLocation_Complete_PostZip((long) Long.parseLong(capturedRow[1]),capturedRow[2]));
			try {
				
				// This is where the actual encoding would take place...
				// for now (since we're already in AAC) we just copy the file to the final location
				fileUtils.copy(preEncodeFile, postEncodeFile);
				if (preEncodeFile.exists() && postEncodeFile.exists()) { preEncodeFile.delete(); }
				
				String digest = fileUtils.sha1Hash(postEncodeFile.getAbsolutePath());
				gZipUtils.gZipFile(postEncodeFile, gZippedFile);
				if (postEncodeFile.exists() && gZippedFile.exists()) { postEncodeFile.delete(); }
				
				app.audioDb.dbCaptured.clearCapturedBefore(dateTimeUtils.getDateFromString(capturedRow[0]));
				app.audioDb.dbEncoded.insert(capturedRow[1], capturedRow[2],digest);
				
				//make sure the previous step(s) are synchronous or else the checkin will occur before the encode...
				app.audioEncode.triggerCheckInAfterEncode(app.getApplicationContext());
				
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}
	
	}

}
