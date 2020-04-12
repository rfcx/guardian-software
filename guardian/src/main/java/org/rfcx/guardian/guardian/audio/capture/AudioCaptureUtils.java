package org.rfcx.guardian.guardian.audio.capture;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.capture.DeviceDiskUsage;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class AudioCaptureUtils {

	public AudioCaptureUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		verifyHardwareSupportForCaptureSampleRate();
		RfcxAudioUtils.initializeAudioDirectories(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCaptureUtils");

	private RfcxGuardian app = null;
	
	public long[] queueCaptureTimeStamp = new long[] { 0, 0 };
	public int[] queueCaptureSampleRate = new int[] { 0, 0 };

	public boolean updateCaptureQueue(long timeStamp, int sampleRate) {

		queueCaptureTimeStamp[0] = queueCaptureTimeStamp[1];
		queueCaptureTimeStamp[1] = timeStamp;

		queueCaptureSampleRate[0] = queueCaptureSampleRate[1];
		queueCaptureSampleRate[1] = sampleRate;

		return (queueCaptureTimeStamp[0] > 0);
	}
	
	public boolean isAudioCaptureHardwareSupported = false;
	
	private void verifyHardwareSupportForCaptureSampleRate() {
		
		int[] defaultSampleRateOptions = new int[] { 8000, 12000, 16000, 24000, 48000 };
		int originalSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate");
		int verifiedOrUpdatedSampleRate = originalSampleRate;
		
		for (int i = 0; i < defaultSampleRateOptions.length; i++) {
			if (		(defaultSampleRateOptions[i] >= originalSampleRate)
				&&	(AudioRecord.getMinBufferSize(defaultSampleRateOptions[i], AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT) > 0)
				) {
					verifiedOrUpdatedSampleRate = defaultSampleRateOptions[i];
					this.isAudioCaptureHardwareSupported = true;
					break;
					
			} else if (i == (defaultSampleRateOptions.length-1)) {
				this.isAudioCaptureHardwareSupported = false;
				Log.e(logTag, "Failed to verify hardware support for any of the provided audio sample rates...");
			}
		}
		
		if (verifiedOrUpdatedSampleRate != originalSampleRate) {
			app.rfcxPrefs.setPref("audio_sample_rate", verifiedOrUpdatedSampleRate);
			Log.e(logTag, "Audio capture sample rate of "+Math.round(originalSampleRate/1000)+" kHz not supported. Sample rate updated to "+Math.round(verifiedOrUpdatedSampleRate/1000)+" kHz.");
		}
		
		Log.v(logTag, "Hardware support verified for audio capture sample rate of "+Math.round(verifiedOrUpdatedSampleRate/1000)+" kHz.");
		
	}
	
	public boolean isBatteryChargeSufficientForCapture() {
		int batteryCharge = this.app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= this.app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff"));
	}

	public boolean isCaptureAllowedAtThisTimeOfDay() {
		for (String offHoursRange : TextUtils.split(app.rfcxPrefs.getPrefAsString("audio_schedule_off_hours"), ",")) {
			String[] offHours = TextUtils.split(offHoursRange, "-");
			if (DateTimeUtils.isTimeStampWithinTimeRange(new Date(), offHours[0], offHours[1])) {
				return false;
			}
		}
		return true;
	}

	private static final int requiredAvailableInternalDiskSpace = 32;
	
	public static boolean isAvailableDiskSpaceSufficientForCapture() {
		return true;
		//	return (DeviceDiskUsage.getInternalDiskFreeMegaBytes() >= requiredAvailableInternalDiskSpace);
	}
	
	public boolean limitBasedOnBatteryLevel() {
		return (!isBatteryChargeSufficientForCapture() && this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_battery"));
	}
	
	public boolean limitBasedOnTimeOfDay() {
		return (!isCaptureAllowedAtThisTimeOfDay() && this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_schedule_off_hours"));
	}
	
	public boolean isAudioCaptureAllowed() {
		
		boolean limitBasedOnBatteryLevel = limitBasedOnBatteryLevel();
		boolean limitBasedOnTimeOfDay = limitBasedOnTimeOfDay();
		boolean limitBasedOnExternalStorage = !FileUtils.isExternalStorageAvailable();
		boolean limitBasedOnLackOfHardwareSupport = !this.isAudioCaptureHardwareSupported;

		// we set this to true, and cycle through conditions that might make it false
		// we then return the resulting true/false value
		boolean isAudioCaptureAllowedUnderKnownConditions = true;
		StringBuilder msgNoCapture = new StringBuilder();

		if (!this.app.rfcxPrefs.getPrefAsBoolean("enable_audio_capture")) {
			msgNoCapture.append("it being explicitly disabled ('enable_audio_capture' is set to false).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (limitBasedOnTimeOfDay) {
			msgNoCapture.append("current time of day/night")
						.append(" (off hours: '").append(app.rfcxPrefs.getPrefAsString("audio_schedule_off_hours")).append("'.");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (limitBasedOnBatteryLevel) {
			msgNoCapture.append("low battery level")
						.append(" (current: ").append(this.app.deviceBattery.getBatteryChargePercentage(this.app.getApplicationContext(), null)).append("%,")
						.append(" required: ").append(this.app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff")).append("%).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (limitBasedOnExternalStorage) {
			msgNoCapture.append("a lack of external storage.")
					//	.append(" (current: ").append(DeviceDiskUsage.getInternalDiskFreeMegaBytes()).append("MB)")
						.append(" (required: ").append(requiredAvailableInternalDiskSpace).append("MB).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (limitBasedOnLackOfHardwareSupport) {
			msgNoCapture.append("lack of hardware support for capture sample rate: ")
						.append(Math.round(app.rfcxPrefs.getPrefAsInt("audio_sample_rate")/1000)).append(" kHz.");
			isAudioCaptureAllowedUnderKnownConditions = false;

		}

		if (!isAudioCaptureAllowedUnderKnownConditions) {
			Log.d(logTag, msgNoCapture.insert(0, DateTimeUtils.getDateTime()+" - AudioCapture not allowed due to ").toString());
		}

		return isAudioCaptureAllowedUnderKnownConditions;
//		return 		this.app.rfcxPrefs.getPrefAsBoolean("enable_audio_capture")
//				&&	!limitBasedOnTimeOfDay
//				&& 	!limitBasedOnBatteryLevel
//				&& 	!limitBasedOnExternalStorage
//				&& 	!limitBasedOnLackOfHardwareSupport;
	}
	
	public static String getCaptureFilePath(String captureDir, long timestamp, String fileExtension) {
		return (new StringBuilder()).append(captureDir).append("/").append(timestamp).append(".").append(fileExtension).toString();
	}
	
	public static AudioCaptureWavRecorder initializeWavRecorder(String captureDir, long timestamp, int sampleRate) throws Exception {
		AudioCaptureWavRecorder wavRecorderInstance = null;
		wavRecorderInstance = AudioCaptureWavRecorder.getInstance(sampleRate);
		wavRecorderInstance.setOutputFile(getCaptureFilePath(captureDir, timestamp, "wav"));
		wavRecorderInstance.prepareRecorder();
        return wavRecorderInstance;
	}
	
	public static boolean reLocateAudioCaptureFile(Context context, long timestamp, String fileExtension) {
		boolean isFileMoved = false;
		File captureFile = new File(getCaptureFilePath(RfcxAudioUtils.captureDir(context),timestamp,fileExtension));
		if (captureFile.exists()) {
			try {
				File preEncodeFile = new File(RfcxAudioUtils.getAudioFileLocation_PreEncode(context, timestamp,fileExtension));
				FileUtils.copy(captureFile, preEncodeFile);
				FileUtils.chmod(preEncodeFile, "rw", "rw");
				if (preEncodeFile.exists()) { captureFile.delete(); }	
				isFileMoved = preEncodeFile.exists();
			} catch (IOException e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return isFileMoved;
	}

}
