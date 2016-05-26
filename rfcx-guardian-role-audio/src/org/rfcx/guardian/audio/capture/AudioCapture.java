package org.rfcx.guardian.audio.capture;

import java.io.File;
import java.io.IOException;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.audio.wav.WavAudioRecorder;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.audio.AudioFile;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.media.MediaRecorder;

public class AudioCapture {

	public AudioCapture(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		AudioFile.initializeAudioDirectories(context);
	}
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioCapture.class.getSimpleName();

	private RfcxGuardian app = null;
	
	public long[] captureTimeStampQueue = new long[] { 0, 0 };

	public boolean updateCaptureTimeStampQueue(long timeStamp) {
		captureTimeStampQueue[0] = captureTimeStampQueue[1];
		captureTimeStampQueue[1] = timeStamp;
		return (captureTimeStampQueue[0] > 0);
	}
	
	public boolean isBatteryChargeSufficientForCapture() {
		int batteryCharge = this.app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= this.app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff"));
	}
	
	private static String getCaptureFilePath(String captureDir, long timestamp, String fileExtension) {
		return (new StringBuilder()).append(captureDir).append("/").append(timestamp).append(".").append(fileExtension).toString();
	}
	
	public static MediaRecorder getAacRecorder(String captureDir, long timestamp, String fileExtension, int bitRate, int sampleRate) throws IllegalStateException, IOException, IllegalThreadStateException {
		MediaRecorder rec = null;
		rec = new MediaRecorder();
        rec.setAudioSource(MediaRecorder.AudioSource.MIC);
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        rec.setAudioSamplingRate(sampleRate);
        rec.setAudioEncodingBitRate(bitRate);
        rec.setAudioChannels(AudioFile.AUDIO_CHANNEL_COUNT);
		rec.setOutputFile(getCaptureFilePath(captureDir,timestamp,fileExtension));
		rec.prepare();
        return rec;
	}
	
	public static WavAudioRecorder getWavRecorder(String captureDir, long timestamp, String fileExtension, int sampleRate) throws IllegalStateException, IOException, IllegalThreadStateException {
		WavAudioRecorder rec = null;
		rec = WavAudioRecorder.getInstance(sampleRate);
		rec.setOutputFile(getCaptureFilePath(captureDir,timestamp,fileExtension));
		rec.prepare();
        return rec;
	}
	
	public static boolean reLocateAudioCaptureFile(Context context, long timestamp, String fileExtension) {
		boolean isFileMoved = false;
		File captureFile = new File(getCaptureFilePath(AudioFile.captureDir(context),timestamp,fileExtension));
		if (captureFile.exists()) {
			try {
				File preEncodeFile = new File(AudioFile.getAudioFileLocation_PreEncode(timestamp,fileExtension));
				FileUtils.copy(captureFile, preEncodeFile);
				FileUtils.chmod(preEncodeFile, 0777);
				if (preEncodeFile.exists()) { captureFile.delete(); }	
				isFileMoved = preEncodeFile.exists();
			} catch (IOException e) {
				RfcxLog.logExc(TAG, e);
			}
		}
		return isFileMoved;
	}

}
