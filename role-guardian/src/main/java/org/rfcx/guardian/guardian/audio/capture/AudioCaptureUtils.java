package org.rfcx.guardian.guardian.audio.capture;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;
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
		RfcxAudioUtils.initializeAudioDirectories(context);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCaptureUtils");

	private RfcxGuardian app = null;

	public long[] queueCaptureTimeStamp = new long[] { 0, 0 };
	public int[] queueCaptureSampleRate = new int[] { 0, 0 };

	public static final int inReducedCaptureModeExtendCaptureCycleByFactorOf = 2;

	private boolean isAudioCaptureHardwareSupported = false;
	private static final int requiredFreeDiskSpaceForAudioCapture = 32;



	public static AudioCaptureWavRecorder initializeWavRecorder(String captureDir, long timestamp, int sampleRate) throws Exception {
		AudioCaptureWavRecorder wavRecorderInstance = null;
		wavRecorderInstance = AudioCaptureWavRecorder.getInstance(sampleRate);
		wavRecorderInstance.setOutputFile(getCaptureFilePath(captureDir, timestamp, "wav"));
		wavRecorderInstance.prepareRecorder();
		return wavRecorderInstance;
	}



	private boolean doesHardwareSupportCaptureSampleRate() {

		if (!this.isAudioCaptureHardwareSupported) {

			int[] defaultSampleRateOptions = new int[]{ 8000, 12000, 16000, 24000, 48000 };
			int originalSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate");
			int verifiedOrUpdatedSampleRate = originalSampleRate;

			for (int i = 0; i < defaultSampleRateOptions.length; i++) {
				if ((defaultSampleRateOptions[i] >= originalSampleRate)
						&& (AudioRecord.getMinBufferSize(defaultSampleRateOptions[i], AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT) > 0)
				) {

					verifiedOrUpdatedSampleRate = defaultSampleRateOptions[i];
					this.isAudioCaptureHardwareSupported = true;
					break;

				} else if (i == (defaultSampleRateOptions.length - 1)) {
					this.isAudioCaptureHardwareSupported = false;
					Log.e(logTag, "Failed to verify hardware support for any of the provided audio sample rates...");
				}
			}

			if (verifiedOrUpdatedSampleRate != originalSampleRate) {
				app.setSharedPref("audio_sample_rate", "" + verifiedOrUpdatedSampleRate);
				Log.e(logTag, "Audio capture sample rate of " + originalSampleRate + " Hz not supported. Sample rate updated to " + verifiedOrUpdatedSampleRate + " Hz.");
				this.isAudioCaptureHardwareSupported = true;
			}

			if (this.isAudioCaptureHardwareSupported) {
				Log.v(logTag, "Hardware support verified for audio capture sample rate of " + verifiedOrUpdatedSampleRate + " Hz.");
			}
		}

		return this.isAudioCaptureHardwareSupported;
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

	private boolean limitBasedOnBatteryLevel() {
		return (!isBatteryChargeSufficientForCapture() && this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_battery"));
	}

	private boolean limitBasedOnTimeOfDay() {
		return (!isCaptureAllowedAtThisTimeOfDay() && this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_schedule_off_hours"));
	}

	private boolean limitBasedOnInternalStorage() {
		return (DeviceDiskUsage.getInternalDiskFreeMegaBytes() <= requiredFreeDiskSpaceForAudioCapture);
	}

//	private boolean limitBasedOnExternalStorage() {
//		return !FileUtils.isExternalStorageAvailable();
//	}

	public JSONArray getAudioCaptureStatusAsJsonArray() {
		JSONArray statusJsonArray = new JSONArray();
        try {
            JSONObject statusObj = new JSONObject();
            statusObj.put("is_allowed", isAudioCaptureAllowed(false));
			statusJsonArray.put(statusObj);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);

        } finally {
            return statusJsonArray;
        }
	}

	public boolean isAudioCaptureAllowed(boolean printFeedbackInLog) {

		// we set this to true, and cycle through conditions that might make it false
		// we then return the resulting true/false value
		boolean isAudioCaptureAllowedUnderKnownConditions = true;
		StringBuilder msgNoCapture = new StringBuilder();

		if (!this.app.rfcxPrefs.getPrefAsBoolean("enable_audio_capture")) {
			msgNoCapture.append("it being explicitly disabled ('enable_audio_capture' is set to false).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (limitBasedOnBatteryLevel()) {
			msgNoCapture.append("low battery level")
					.append(" (current: ").append(this.app.deviceBattery.getBatteryChargePercentage(this.app.getApplicationContext(), null)).append("%,")
					.append(" required: ").append(this.app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff")).append("%).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (limitBasedOnTimeOfDay()) {
			msgNoCapture.append("current time of day/night")
						.append(" (off hours: '").append(app.rfcxPrefs.getPrefAsString("audio_schedule_off_hours")).append("'.");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (limitBasedOnInternalStorage()) {
			msgNoCapture.append("a lack of sufficient free internal disk storage.")
						.append(" (current: ").append(DeviceDiskUsage.getInternalDiskFreeMegaBytes()).append("MB)")
						.append(" (required: ").append(requiredFreeDiskSpaceForAudioCapture).append("MB).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (!doesHardwareSupportCaptureSampleRate()) {
			msgNoCapture.append("lack of hardware support for capture sample rate: ")
						.append(app.rfcxPrefs.getPrefAsInt("audio_sample_rate")).append(" Hz.");
			isAudioCaptureAllowedUnderKnownConditions = false;

		}

		if (!isAudioCaptureAllowedUnderKnownConditions) {
			if (printFeedbackInLog) {
				Log.d(logTag, msgNoCapture
						.insert(0, DateTimeUtils.getDateTime() + " - AudioCapture not allowed due to ")
						.append(" Waiting ").append(app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") * inReducedCaptureModeExtendCaptureCycleByFactorOf).append(" seconds before next attempt.")
						.toString());
			}
		}

		return isAudioCaptureAllowedUnderKnownConditions;
	}



	public boolean updateCaptureQueue(long timeStamp, int sampleRate) {

		queueCaptureTimeStamp[0] = queueCaptureTimeStamp[1];
		queueCaptureTimeStamp[1] = timeStamp;

		queueCaptureSampleRate[0] = queueCaptureSampleRate[1];
		queueCaptureSampleRate[1] = sampleRate;

		return (queueCaptureTimeStamp[0] > 0);
	}

	public static String getCaptureFilePath(String captureDir, long timestamp, String fileExtension) {
		return (new StringBuilder()).append(captureDir).append("/").append(timestamp).append(".").append(fileExtension).toString();
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
