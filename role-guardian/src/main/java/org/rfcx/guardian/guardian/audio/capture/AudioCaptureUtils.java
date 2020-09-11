package org.rfcx.guardian.guardian.audio.capture;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.capture.DeviceDiskUsage;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import org.rfcx.guardian.guardian.RfcxGuardian;

public class AudioCaptureUtils {

	public AudioCaptureUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		RfcxAudioUtils.initializeAudioDirectories(context);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCaptureUtils");

	private RfcxGuardian app = null;

	public String[] samplingRatioStrArr = new String[] {};
	public int[] samplingRatioArr = new int[] {};
	public int samplingRatioIteration = 0;

	public long[] queueCaptureTimeStamp = new long[] { 0, 0 };
	public int[] queueCaptureSampleRate = new int[] { 0, 0 };

	private boolean isAudioCaptureHardwareSupported = false;
	private static final int requiredFreeDiskSpaceForAudioCapture = 32;

	private static AudioCaptureWavRecorder wavRecorderForCompanion = null;

	public static AudioCaptureWavRecorder initializeWavRecorder(String captureDir, long timestamp, int sampleRate) throws Exception {
		wavRecorderForCompanion = AudioCaptureWavRecorder.getInstance(sampleRate);;
		wavRecorderForCompanion.setOutputFile(getCaptureFilePath(captureDir, timestamp, "wav"));
		wavRecorderForCompanion.prepareRecorder();
		return wavRecorderForCompanion;
	}

	public void updateSamplingRatioIteration() {
		this.samplingRatioStrArr = TextUtils.split(app.rfcxPrefs.getPrefAsString("audio_sampling_ratio"), ":");
		this.samplingRatioArr = new int[] { Integer.parseInt(this.samplingRatioStrArr[0]), Integer.parseInt(this.samplingRatioStrArr[1]) };
		if (this.samplingRatioIteration > this.samplingRatioArr[1]) { this.samplingRatioIteration = 0; }
		this.samplingRatioIteration++;
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

	private boolean isBatteryChargeSufficientForCapture() {
		int batteryCharge = this.app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= this.app.rfcxPrefs.getPrefAsInt("audio_cutoff_battery"));
	}

	private boolean limitBasedOnBatteryLevel() {
		return (!isBatteryChargeSufficientForCapture() && this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_battery"));
	}


	private boolean limitBasedOnSentinelBatteryLevel() {

		if (!this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_sentinel_battery")) {
			return false;
		} else {
			try {
				JSONArray jsonArray = RfcxComm.getQueryContentProvider("admin", "status", "*", app.getApplicationContext().getContentResolver());
				if (jsonArray.length() > 0) {
					JSONObject jsonObj = jsonArray.getJSONObject(0);
					if (jsonObj.has("audio_capture")) {
						JSONObject audioCaptureObj = jsonObj.getJSONObject("audio_capture");
						if (audioCaptureObj.has("is_allowed")) {
							return !audioCaptureObj.getBoolean(("is_allowed"));
						}
					}
				}
			} catch (JSONException e) {
				RfcxLog.logExc(logTag, e);
			}
		}
		return false;
	}

	private boolean limitBasedOnInternalStorage() {
		return (DeviceDiskUsage.getInternalDiskFreeMegaBytes() <= requiredFreeDiskSpaceForAudioCapture);
	}




	private boolean isCaptureAllowedAtThisTimeOfDay() {
		for (String offHoursRange : TextUtils.split(app.rfcxPrefs.getPrefAsString("audio_schedule_off_hours"), ",")) {
			String[] offHours = TextUtils.split(offHoursRange, "-");
			if (DateTimeUtils.isTimeStampWithinTimeRange(new Date(), offHours[0], offHours[1])) {
				return false;
			}
		}
		return true;
	}

	private boolean limitBasedOnTimeOfDay() {
		return (!isCaptureAllowedAtThisTimeOfDay() && this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_schedule_off_hours"));
	}

	private boolean limitBasedOnCaptureSamplingRatio() {
		return (!isCaptureAllowedAtThisSamplingRatioIteration() && this.app.rfcxPrefs.getPrefAsBoolean("enable_cutoffs_sampling_ratio"));
	}

	private boolean isCaptureAllowedAtThisSamplingRatioIteration() {
		return (this.samplingRatioIteration == 1);
	}

	public JSONObject audioCaptureStatusAsJsonObj() {
		JSONObject statusObj = null;
        try {
            statusObj = new JSONObject();
            statusObj.put("is_allowed", isAudioCaptureAllowed(false, false));
			statusObj.put("is_blocked", isAudioCaptureBlocked(false));
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return statusObj;
	}

	public boolean isAudioCaptureAllowed(boolean includeSentinel, boolean printFeedbackInLog) {

		// we set this to true, and cycle through conditions that might make it false
		// we then return the resulting true/false value
		boolean isAudioCaptureAllowedUnderKnownConditions = true;
		StringBuilder msgNoCapture = new StringBuilder();

		if (limitBasedOnBatteryLevel()) {
			msgNoCapture.append("Low Battery level")
					.append(" (current: ").append(this.app.deviceBattery.getBatteryChargePercentage(this.app.getApplicationContext(), null)).append("%,")
					.append(" required: ").append(this.app.rfcxPrefs.getPrefAsInt("audio_cutoff_battery")).append("%).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (includeSentinel && limitBasedOnSentinelBatteryLevel()) {
				msgNoCapture.append("Low Sentinel Battery level")
						.append(" (required: ").append(this.app.rfcxPrefs.getPrefAsInt("audio_cutoff_sentinel_battery")).append("%).");
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
						.append(" Waiting ").append( 2 * app.rfcxPrefs.getPrefAsInt("audio_cycle_duration")).append(" seconds before next attempt.")
						.toString());
			}
		}

		return isAudioCaptureAllowedUnderKnownConditions;
	}

	public boolean isAudioCaptureBlocked(boolean printFeedbackInLog) {

		// we set this to false, and cycle through conditions that might make it true
		// we then return the resulting true/false value
		boolean isAudioCaptureBlockedRightNow = false;
		StringBuilder msgNoCapture = new StringBuilder();

		if (!this.app.rfcxPrefs.getPrefAsBoolean("enable_audio_capture")) {
			msgNoCapture.append("it being explicitly disabled ('enable_audio_capture' is set to false).");
			isAudioCaptureBlockedRightNow = true;

		} else if (limitBasedOnTimeOfDay()) {
			msgNoCapture.append("current time of day/night")
					.append(" (off hours: '").append(app.rfcxPrefs.getPrefAsString("audio_schedule_off_hours")).append("'.");
			isAudioCaptureBlockedRightNow = true;

		} else if (limitBasedOnCaptureSamplingRatio()) {
			msgNoCapture.append("a sampling ratio definition. ")
						.append("Ratio is '").append(app.rfcxPrefs.getPrefAsString("audio_sampling_ratio")).append("'. ")
						.append("Currently on iteration ").append(this.samplingRatioIteration).append(" of ").append(this.samplingRatioArr[0]+this.samplingRatioArr[1]).append(".");
			isAudioCaptureBlockedRightNow = true;

		}

		if (isAudioCaptureBlockedRightNow) {
			if (printFeedbackInLog) {
				Log.d(logTag, msgNoCapture
						.insert(0, DateTimeUtils.getDateTime() + " - AudioCapture paused due to ")
						.append(" Waiting ").append(app.rfcxPrefs.getPrefAsInt("audio_cycle_duration")).append(" seconds before next attempt.")
						.toString());
			}
		}

		return isAudioCaptureBlockedRightNow;
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
		File captureFile = new File(getCaptureFilePath(RfcxAudioUtils.audioCaptureDir(context),timestamp,fileExtension));
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

	public Pair<byte[], Integer> getAudioBuffer() {
		return wavRecorderForCompanion.getAudioBuffer();
	}

	public Boolean isAudioChanged() {
		return wavRecorderForCompanion.isAudioChanged();
	}

}
