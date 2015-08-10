package org.rfcx.guardian.audio.capture;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.audio.database.AudioDb;
import org.rfcx.guardian.utility.RfcxConstants;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class AudioCapture {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+AudioCapture.class.getSimpleName();

	public String captureDir = null;
	
	// encode straight to AAC/M4A (lossy, constant bitrate)...
	// ...or don't, and encode asynchronously after
	// (*may* eventually support various lossy formats for post-encoding)
	public static final boolean ENCODE_ON_CAPTURE = true;
	
	public final static int CAPTURE_SAMPLE_RATE_HZ = 8000;
	
	public void initializeAudioDirectories(RfcxGuardian app) {
		String appFilesDir = app.getApplicationContext().getFilesDir().toString();
		String finalFilesDir = appFilesDir;
		(new File(app.audioEncode.sdCardFilesDir)).mkdirs();
		
		if ((new File(app.audioEncode.sdCardFilesDir)).isDirectory()) { finalFilesDir = app.audioEncode.sdCardFilesDir; }
				
		this.captureDir = appFilesDir+"/capture"; (new File(this.captureDir)).mkdirs();
		app.audioEncode.encodeDir = appFilesDir+"/encode"; (new File(app.audioEncode.encodeDir)).mkdirs();
		app.audioEncode.aacDir = finalFilesDir+"/m4a"; (new File(app.audioEncode.aacDir)).mkdirs();
		
	}
	
	public void cleanupCaptureDirectory() {
		for (File file : (new File(this.captureDir)).listFiles()) {
			try { file.delete(); } catch (Exception e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC); }
		}
	}
	
//	public void purgeEncodedAssetsUpTo(AudioDb audioDb, Date purgeUpTo) {		
//		List<String[]> encodedAudioEntries = audioDb.dbEncoded.getAllEncoded();
//		for (String[] encodedAudioEntry : encodedAudioEntries) {
//			try {
//				(new File(this.wavDir.substring(0,this.wavDir.lastIndexOf("/"))+"/"+encodedAudioEntry[2]+"/"+encodedAudioEntry[1]+"."+encodedAudioEntry[2])).delete();
//			} catch (Exception e) {
//				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//			}
//		}
//		audioDb.dbCaptured.clearCapturedBefore(purgeUpTo);
//		audioDb.dbEncoded.clearEncodedBefore(purgeUpTo);
//	}
	
//	public void purgeAllAudioAssets(AudioDb audioDb) {
//		Log.d(TAG, "Purging all existing audio assets...");
//		try {
//			for (String audioDir : this.audioDirectories) {
//				if (!audioDir.equals(captureDir)) { //with this, we can purge audio assets on-the-fly, even during capture
//					for (File file : (new File(audioDir)).listFiles()) { file.delete(); }
//				}
//			}
//		} catch (Exception e) {
//			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//		}
//		audioDb.dbCaptured.clearCapturedBefore(new Date());
//		audioDb.dbEncoded.clearEncodedBefore(new Date());
//	}
	

	
//	public boolean mayEncodeOnCapture() {
//		return this.ENCODE_ON_CAPTURE;
//	}

}
