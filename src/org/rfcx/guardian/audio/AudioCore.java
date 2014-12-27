package org.rfcx.guardian.audio;

import java.io.File;
import java.util.Date;

import net.sourceforge.javaFlacEncoder.FLAC_FileEncoder;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.database.AudioDb;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;


public class AudioCore {

	private static final String TAG = AudioCore.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	public String captureDir = null;
	public String flacDir = null;
	public String aacDir = null;
	public String wavDir = null;
	private String[] audioDirectories = new String[] {null, null, null, null};
	
	// encode straight to AAC/M4A (lossy, constant bitrate)...
	// ...or don't, and encode asynchronously after (may eventually support many formats for post-encoding)
	private boolean encodeOnCapture = true;
	
	public boolean purgeAudioAssetsOnStart = false;
	
	public final static int CAPTURE_SAMPLE_RATE_HZ = 8000;
	public final int CAPTURE_LOOP_PERIOD_SECS = 150;
	public final int aacEncodingBitRate = 16384;
	
	public void encodeCaptureAudio(String fileName, String encodedFormat, String dbRowEntryDate, AudioDb audioDb) {
		File wavFile = new File(wavDir+"/"+fileName+".wav");
		File encodedFile = new File(wavDir.substring(0,wavDir.lastIndexOf("/"))+"/"+encodedFormat+"/"+fileName+"."+encodedFormat);
		try {
			if (encodedFormat == "flac") {
				FLAC_FileEncoder ffe = new FLAC_FileEncoder();
				ffe.adjustAudioConfig(CAPTURE_SAMPLE_RATE_HZ, 16, 1);
				ffe.encode(wavFile, encodedFile);
			}
		} catch(Exception e) {
			Log.e(TAG,(e!=null) ? TextUtils.join(" | ", e.getStackTrace()) : NULL_EXC);
		} finally {
			if (wavFile.exists()) {
				wavFile.delete();
				audioDb.dbCaptured.clearCapturedBefore(audioDb.dateTimeUtils.getDateFromString(dbRowEntryDate));
			}
			if (encodedFile.exists()) {
				audioDb.dbEncoded.insert(fileName, encodedFormat);
			}
		}
	}
	
	public void initializeAudioDirectories(RfcxGuardian app) {
		this.captureDir = app.filesDir+"/capture"; this.audioDirectories[0] = this.captureDir;
		this.wavDir = app.filesDir+"/wav"; this.audioDirectories[1] = this.wavDir;
		this.flacDir = app.filesDir+"/flac"; this.audioDirectories[2] = this.flacDir;
		this.aacDir = app.filesDir+"/m4a"; this.audioDirectories[3] = this.aacDir;
		
		for (String audioDir : this.audioDirectories) { (new File(audioDir)).mkdirs(); }
	}
	
	public void cleanupCaptureDirectory() {
		for (File file : (new File(this.captureDir)).listFiles()) {
			try { file.delete(); } catch (Exception e) { Log.e(TAG,(e!=null) ? TextUtils.join(" | ", e.getStackTrace()) : NULL_EXC); }
		}
	}
	
	public void purgeAudioAssets(AudioDb audioDb) {
		Log.d(TAG, "Purging all existing audio assets...");
		for (String audioDir : this.audioDirectories) {
			if (!audioDir.equals(captureDir)) { //with this, we can purge audio assets on-the-fly, even during capture
				for (File file : (new File(audioDir)).listFiles()) {
					try { file.delete(); } catch (Exception e) { Log.e(TAG,(e!=null) ? TextUtils.join(" | ", e.getStackTrace()) : NULL_EXC); }
				}
			}
		}
		audioDb.dbCaptured.clearCapturedBefore(new Date());
		audioDb.dbEncoded.clearEncodedBefore(new Date());
	}
	
	public void queueAudioCaptureFollowUp(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent audioEncodeIntentService = PendingIntent.getService(context, -1, new Intent(context, AudioEncodeIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.set(AlarmManager.RTC, System.currentTimeMillis(), audioEncodeIntentService);
	}
	
	public boolean mayEncodeOnCapture() {
		return this.encodeOnCapture;
	}
	
	public void setEncodeOnCapture(boolean trueFalse) {
		this.encodeOnCapture = trueFalse;
	}
}
