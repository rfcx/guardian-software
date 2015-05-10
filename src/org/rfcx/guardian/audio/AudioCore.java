package org.rfcx.guardian.audio;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.sourceforge.javaFlacEncoder.FLAC_FileEncoder;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.database.AudioDb;
import org.rfcx.guardian.intentservice.AudioEncode;
import org.rfcx.guardian.utility.FileUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class AudioCore {

	private static final String TAG = "RfcxGuardian-"+AudioCore.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	public String captureDir = null;
	public String flacDir = null;
	public String aacDir = null;
	public String wavDir = null;
	private String[] audioDirectories = new String[] {null, null, null, null};
	
	// encode straight to AAC/M4A (lossy, constant bitrate)...
	// ...or don't, and encode asynchronously after
	// (*may* eventually support various lossy formats for post-encoding)
	private boolean encodeOnCapture = true;
	
	public boolean purgeAudioAssetsOnStart = false;
	
	public final static int CAPTURE_SAMPLE_RATE_HZ = 8000;
//	public final int aacEncodingBitRate = 12288;
	public final int aacEncodingBitRate = 16384;
	
	public void encodeCaptureAudio(String fileName, String encodedFormat, String dbRowEntryDate, AudioDb audioDb) {
		File wavFile = new File(wavDir+"/"+fileName+".wav");
		String encodedFilePath = wavDir.substring(0,wavDir.lastIndexOf("/"))+"/"+encodedFormat+"/"+fileName+"."+encodedFormat;
		File encodedFile = new File(encodedFilePath);
		try {
			if (encodedFormat == "flac") {
				FLAC_FileEncoder ffe = new FLAC_FileEncoder();
				ffe.adjustAudioConfig(CAPTURE_SAMPLE_RATE_HZ, 16, 1);
				ffe.encode(wavFile, encodedFile);
			}
		} catch(Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		} finally {
			if (wavFile.exists()) {
				wavFile.delete();
				audioDb.dbCaptured.clearCapturedBefore(audioDb.dateTimeUtils.getDateFromString(dbRowEntryDate));
			}
			if (encodedFile.exists()) {
				String digest = (new FileUtils()).sha1Hash(encodedFilePath);
				audioDb.dbEncoded.insert(fileName, encodedFormat, digest);
			}
		}
	}
	
	public void initializeAudioCapture(RfcxGuardian app) {
		initializeAudioDirectories(app);
		if (app.audioCore.purgeAudioAssetsOnStart) {
			app.audioCore.purgeAllAudioAssets(app.audioDb);
		}
	}
	
	private void initializeAudioDirectories(RfcxGuardian app) {
		String filesDir = app.getApplicationContext().getFilesDir().toString();
		String encodedFilesDir = filesDir;
		
		String sdCardFilesDir = Environment.getExternalStorageDirectory().toString()+"/rfcx";
		(new File(sdCardFilesDir)).mkdirs();
		if ((new File(sdCardFilesDir)).isDirectory()) { encodedFilesDir = sdCardFilesDir; }
				
		this.captureDir = filesDir+"/capture"; this.audioDirectories[0] = this.captureDir;
		this.wavDir = encodedFilesDir+"/wav"; this.audioDirectories[1] = this.wavDir;
		this.flacDir = encodedFilesDir+"/flac"; this.audioDirectories[2] = this.flacDir;
		this.aacDir = encodedFilesDir+"/m4a"; this.audioDirectories[3] = this.aacDir;
		
		for (String audioDir : this.audioDirectories) { (new File(audioDir)).mkdirs(); }
	}
	
	public void cleanupCaptureDirectory() {
		for (File file : (new File(this.captureDir)).listFiles()) {
			try { file.delete(); } catch (Exception e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC); }
		}
	}
	
	public void purgeEncodedAssetsUpTo(AudioDb audioDb, Date purgeUpTo) {		
		List<String[]> encodedAudioEntries = audioDb.dbEncoded.getAllEncoded();
		for (String[] encodedAudioEntry : encodedAudioEntries) {
			try {
				(new File(this.wavDir.substring(0,this.wavDir.lastIndexOf("/"))+"/"+encodedAudioEntry[2]+"/"+encodedAudioEntry[1]+"."+encodedAudioEntry[2])).delete();
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			}
		}
		audioDb.dbCaptured.clearCapturedBefore(purgeUpTo);
		audioDb.dbEncoded.clearEncodedBefore(purgeUpTo);
	}
	
	public void purgeAllAudioAssets(AudioDb audioDb) {
		Log.d(TAG, "Purging all existing audio assets...");
		try {
			for (String audioDir : this.audioDirectories) {
				if (!audioDir.equals(captureDir)) { //with this, we can purge audio assets on-the-fly, even during capture
					for (File file : (new File(audioDir)).listFiles()) { file.delete(); }
				}
			}
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		audioDb.dbCaptured.clearCapturedBefore(new Date());
		audioDb.dbEncoded.clearEncodedBefore(new Date());
	}
	
	public void purgeSingleAudioAsset(AudioDb audioDb, String audioTimestamp) {
		Log.d(TAG, "Purging single audio asset: "+audioTimestamp);

		List<String[]> encodedAudioEntries = audioDb.dbEncoded.getAllEncoded();
		for (String[] encodedAudioEntry : encodedAudioEntries) {
			if (encodedAudioEntry[1].equals(audioTimestamp)) {
				try {
					(new File(this.wavDir.substring(0,this.wavDir.lastIndexOf("/"))+"/"+encodedAudioEntry[2]+"/"+encodedAudioEntry[1]+"."+encodedAudioEntry[2])).delete();
				} catch (Exception e) {
					Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
				}
			}
		}

		audioDb.dbEncoded.deleteSingleEncoded(audioTimestamp);
	}
	
	public void queueAudioCaptureFollowUp(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent audioEncodeIntentService = PendingIntent.getService(context, -1, new Intent(context, AudioEncode.class), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.set(AlarmManager.RTC, System.currentTimeMillis(), audioEncodeIntentService);
	}
	
	public boolean mayEncodeOnCapture() {
		return this.encodeOnCapture;
	}
	
	public void setEncodeOnCapture(boolean trueFalse) {
		this.encodeOnCapture = trueFalse;
	}
}
