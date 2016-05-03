package org.rfcx.guardian.audio.service;

import java.io.File;
import java.util.Date;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.GZipUtils;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class AudioEncodeIntentService extends IntentService {
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioEncodeIntentService.class.getSimpleName();
	
	public static final String INTENT_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".AUDIO_ENCODE";
	public static final String NOTIFICATION_TAG = "org.rfcx.guardian."+RfcxGuardian.APP_ROLE.toLowerCase()+".RECEIVE_AUDIO_ENCODE_NOTIFICATIONS";

    private FileUtils fileUtils = new FileUtils();
    private GZipUtils gZipUtils = new GZipUtils();
    
	public AudioEncodeIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Intent intent = new Intent(INTENT_TAG);
		sendBroadcast(intent, NOTIFICATION_TAG);
		
		for (String[] capturedRow : app.audioDb.dbCaptured.getAllCaptured()) {
			
			Date encodeStartTime = new Date();
			
			Log.i(TAG, "Encoding: '"+capturedRow[0]+"','"+capturedRow[1]+"','"+capturedRow[2]+"'");
			
			File preEncodeFile = new File(app.audioEncode.getAudioFileLocation_PreEncode((long) Long.parseLong(capturedRow[1]),capturedRow[2]));
			File postEncodeFile = new File(app.audioEncode.getAudioFileLocation_PostEncode((long) Long.parseLong(capturedRow[1]),capturedRow[2]));
			File gZippedFile = new File(app.audioEncode.getAudioFileLocation_Complete_PostZip((long) Long.parseLong(capturedRow[1]),capturedRow[2]));
			
			try {
				
				// This is where the actual encoding would take place...
				// for now (since we're already in AAC) we just copy the file to the final location
				fileUtils.copy(preEncodeFile, postEncodeFile);
				
				// delete pre-encode file
				if (preEncodeFile.exists() && postEncodeFile.exists()) { preEncodeFile.delete(); }
				
				// generate file checksum of encoded file
				String preZipDigest = fileUtils.sha1Hash(postEncodeFile.getAbsolutePath());
				
				// GZIP encoded file into final filepath
				gZipUtils.gZipFile(postEncodeFile, gZippedFile);
				
				// If successful, cleanup pre-GZIP file and make sure final file is accessible by other roles (like 'api')
				if (gZippedFile.exists()) {
					fileUtils.chmod(gZippedFile, 0755);
					if (postEncodeFile.exists()) { postEncodeFile.delete(); }
				}
				
				// add encoded audio entry to database
				app.audioDb.dbEncoded.insert(
						capturedRow[1], capturedRow[2], preZipDigest,
						(int) Integer.parseInt(capturedRow[4]), 
						app.rfcxPrefs.getPrefAsInt("audio_encode_bitrate"), 
						app.rfcxPrefs.getPrefAsString("audio_encode_codec"), 
						(long) Long.parseLong(capturedRow[7]),
						(System.currentTimeMillis() - encodeStartTime.getTime())
						);

				// remove capture file entry from database
				app.audioDb.dbCaptured.clearCapturedBefore(new Date((long) Long.parseLong(capturedRow[0])));
				
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
