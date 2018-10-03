package guardian.audio.capture;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.DeviceDiskUsage;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.rfcx.RfcxLog;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.text.TextUtils;
import android.util.Log;
import guardian.RfcxGuardian;

public class AudioCaptureUtils {

	public AudioCaptureUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		verifyHardwareSupportForCaptureSampleRate();
		RfcxAudioUtils.initializeAudioDirectories(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioCaptureUtils.class);

	private RfcxGuardian app = null;
	
	public long[] captureTimeStampQueue = new long[] { 0, 0 };

	public boolean updateCaptureTimeStampQueue(long timeStamp) {
		captureTimeStampQueue[0] = captureTimeStampQueue[1];
		captureTimeStampQueue[1] = timeStamp;
		return (captureTimeStampQueue[0] > 0);
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
	
	public boolean isAudioCaptureAllowed() {
		
		boolean limitBasedOnBatteryLevel = (!isBatteryChargeSufficientForCapture() && this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_battery"));
		boolean limitBasedOnTimeOfDay = (!isCaptureAllowedAtThisTimeOfDay() && this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_schedule_off_hours"));
		boolean limitBasedOnExternalStorage = !FileUtils.isExternalStorageAvailable();
		boolean limitBasedOnLackOfHardwareSupport = !this.isAudioCaptureHardwareSupported;
		
		if (app.rfcxPrefs.getPrefAsBoolean("verbose_logging")) {
			
			String msgNoCapture = null;
			
			if (limitBasedOnTimeOfDay) {
				msgNoCapture = "current time of day/night"
							+" (off hours: '"+app.rfcxPrefs.getPrefAsString("audio_schedule_off_hours")+"'.";
				
			} else if (limitBasedOnBatteryLevel) {
				msgNoCapture = "low battery level"
							+" (current: "+this.app.deviceBattery.getBatteryChargePercentage(this.app.getApplicationContext(), null)+"%,"
							+" required: "+this.app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff")+"%).";
			
			} else if (limitBasedOnExternalStorage) {
				msgNoCapture = "a lack of external storage."
						/*	+" (current: "+DeviceDiskUsage.getInternalDiskFreeMegaBytes()+"MB,"
							+" required: "+requiredAvailableInternalDiskSpace+"MB)."*/;
				
			} else if (limitBasedOnLackOfHardwareSupport) {
				msgNoCapture = "lack of hardware support for capture sample rate: "
							+Math.round(app.rfcxPrefs.getPrefAsInt("audio_sample_rate")/1000)+" kHz.";
				
			}
			
			if (msgNoCapture != null) { Log.d(logTag, DateTimeUtils.getDateTime()+" - AudioCapture not allowed due to "+msgNoCapture); }
			
		}
		
		return 		!limitBasedOnTimeOfDay 
				&& 	!limitBasedOnBatteryLevel 
				&& 	!limitBasedOnExternalStorage 
				&& 	!limitBasedOnLackOfHardwareSupport;
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
				FileUtils.chmod(preEncodeFile, 0777);
				if (preEncodeFile.exists()) { captureFile.delete(); }	
				isFileMoved = preEncodeFile.exists();
			} catch (IOException e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return isFileMoved;
	}

}
