package org.rfcx.guardian.guardian.audio.capture;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import org.rfcx.guardian.audio.wav.WavUtils;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class AudioCaptureUtils {

	public AudioCaptureUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		RfcxAudioFileUtils.initializeAudioDirectories(context);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCaptureUtils");

	private final RfcxGuardian app;

	private static final int[] allowedCaptureSampleRates = new int[]{ 8000, 12000, 16000, 24000, 48000 };

	public String[] samplingRatioStrArr = new String[] {};
	public int[] samplingRatioArr = new int[] {};
	public int samplingRatioIteration = 0;

	public long[] queueCaptureTimestamp_File = new long[] { 0, 0 };
	public long[] queueCaptureTimestamp_Actual = new long[] { 0, 0 };
	public int[] queueCaptureSampleRate = new int[] { 0, 0 };

	private static final int requiredFreeDiskSpaceForAudioCapture = 64;

	private static AudioCaptureWavRecorder wavRecorderForCompanion = null;

	public static AudioCaptureWavRecorder initializeWavRecorder(String captureDir, long timestamp, int sampleRate) throws Exception {
		wavRecorderForCompanion = AudioCaptureWavRecorder.getInstance(sampleRate);
		wavRecorderForCompanion.setOutputFile(getCaptureFilePath(captureDir, timestamp, "wav"));
		wavRecorderForCompanion.prepareRecorder();
		return wavRecorderForCompanion;
	}

	public void updateSamplingRatioIteration() {
		this.samplingRatioStrArr = TextUtils.split(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_SAMPLING_RATIO), ":");
		this.samplingRatioArr = new int[] { Integer.parseInt(this.samplingRatioStrArr[0]), Integer.parseInt(this.samplingRatioStrArr[1]) };
		if (this.samplingRatioIteration > this.samplingRatioArr[1]) { this.samplingRatioIteration = 0; }
		this.samplingRatioIteration++;
	}


	public int getRequiredCaptureSampleRate() {

		int minRequiredSampleRate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CAST_SAMPLE_RATE_MINIMUM);

		if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_STREAM)) {
			minRequiredSampleRate = Math.max(minRequiredSampleRate, app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_STREAM_SAMPLE_RATE));
		}

		if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_VAULT)) {
			minRequiredSampleRate = Math.max(minRequiredSampleRate, app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_VAULT_SAMPLE_RATE));
		}

		if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_CLASSIFY) && (app.audioClassifierDb.dbActive.getCount() > 0) ) {
			minRequiredSampleRate = Math.max(minRequiredSampleRate, app.audioClassifierDb.dbActive.getMaxSampleRateAmongstAllRows());
		}

		return verifyOrUpdateCaptureSampleRateHardwareSupport(minRequiredSampleRate);
	}

	private static int verifyOrUpdateCaptureSampleRateHardwareSupport(int targetSampleRate) {

		int verifiedOrUpdatedSampleRate = targetSampleRate;
		for (int i = 0; i < allowedCaptureSampleRates.length; i++) {
			if ((allowedCaptureSampleRates[i] >= targetSampleRate)
					&& (AudioRecord.getMinBufferSize(allowedCaptureSampleRates[i], AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT) > 0)
			) {
				verifiedOrUpdatedSampleRate = allowedCaptureSampleRates[i];
				break;

			} else if (i == (allowedCaptureSampleRates.length - 1)) {
				Log.e(logTag, "Failed to verify hardware support for any of the provided audio sample rates...");
			}
		}

		if (verifiedOrUpdatedSampleRate != targetSampleRate) {
			Log.e(logTag, "Audio capture sample rate of " + targetSampleRate + " Hz not supported. Sample rate updated to " + verifiedOrUpdatedSampleRate + " Hz.");
		}
		return verifiedOrUpdatedSampleRate;
	}

	private boolean isBatteryChargeSufficientForCapture() {
		int batteryChargeCutoff = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CUTOFF_INTERNAL_BATTERY);
		int batteryCharge = app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		boolean isBatteryChargeSufficient = (batteryCharge >= batteryChargeCutoff);
		if (isBatteryChargeSufficient && (batteryChargeCutoff == 100)) {
			isBatteryChargeSufficient = app.deviceBattery.isBatteryCharged(app.getApplicationContext(), null);
			if (!isBatteryChargeSufficient) { Log.d(logTag, "Battery is at 100% but is not yet fully charged."); }
		}
		return isBatteryChargeSufficient;
	}

	private boolean limitBasedOnBatteryLevel() {
		return (!isBatteryChargeSufficientForCapture() && app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CUTOFFS_INTERNAL_BATTERY));
	}


	private boolean limitBasedOnInternalStorage() {
		return (DeviceStorage.getInternalDiskFreeMegaBytes() <= requiredFreeDiskSpaceForAudioCapture);
	}




	private boolean isCaptureAllowedAtThisTimeOfDay() {
		for (String offHoursRange : TextUtils.split(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_CAPTURE_SCHEDULE_OFF_HOURS), ",")) {
			String[] offHours = TextUtils.split(offHoursRange, "-");
			if (DateTimeUtils.isTimeStampWithinTimeRange(new Date(), offHours[0], offHours[1])) {
				return false;
			}
		}
		return true;
	}

	private boolean limitBasedOnTimeOfDay() {
		return (!isCaptureAllowedAtThisTimeOfDay() && app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CUTOFFS_SCHEDULE_OFF_HOURS));
	}

	private boolean limitBasedOnCaptureSamplingRatio() {
		return (!isCaptureAllowedAtThisSamplingRatioIteration() && app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CUTOFFS_SAMPLING_RATIO));
	}

	private boolean isCaptureAllowedAtThisSamplingRatioIteration() {
		return (this.samplingRatioIteration == 1);
	}

	public boolean isAudioCaptureAllowed(boolean includeSentinel, boolean printFeedbackInLog) {

		// we set this to true, and cycle through conditions that might make it false
		// we then return the resulting true/false value
		boolean isAudioCaptureAllowedUnderKnownConditions = true;

		StringBuilder msgNoCapture = new StringBuilder();

		if (limitBasedOnBatteryLevel()) {
			msgNoCapture.append("Low Battery level")
					.append(" (current: ").append(app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null)).append("%,")
					.append(" required: ").append(app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CUTOFF_INTERNAL_BATTERY)).append("%).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (includeSentinel && !app.rfcxStatus.getFetchedStatus( RfcxStatus.Group.AUDIO_CAPTURE, RfcxStatus.Type.ALLOWED)) {
			msgNoCapture.append("Low Sentinel Battery level")
					.append(" (required: ").append(app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CUTOFF_SENTINEL_BATTERY)).append("%).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		} else if (limitBasedOnInternalStorage()) {
			msgNoCapture.append("a lack of sufficient free internal disk storage.")
					.append(" (current: ").append(DeviceStorage.getInternalDiskFreeMegaBytes()).append("MB)")
					.append(" (required: ").append(requiredFreeDiskSpaceForAudioCapture).append("MB).");
			isAudioCaptureAllowedUnderKnownConditions = false;

		}

		if (!isAudioCaptureAllowedUnderKnownConditions && printFeedbackInLog) {
			Log.d(logTag, msgNoCapture
					.insert(0, DateTimeUtils.getDateTime() + " - AudioCapture not allowed due to ")
					.append(" Waiting ").append(app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION)).append(" seconds before next attempt.")
					.toString());
		}

		return isAudioCaptureAllowedUnderKnownConditions;
	}

	public boolean isAudioCaptureDisabled(boolean printFeedbackInLog) {

		// we set this to false, and cycle through conditions that might make it true
		// we then return the resulting true/false value
		boolean isAudioCaptureDisabledRightNow = false;

		StringBuilder msgNoCapture = new StringBuilder();

		if (!app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_CAPTURE)) {
			msgNoCapture.append("it being explicitly disabled ('" + RfcxPrefs.Pref.ENABLE_AUDIO_CAPTURE.toLowerCase() + "' is set to false).");
			isAudioCaptureDisabledRightNow = true;

		} else if (limitBasedOnTimeOfDay()) {
			msgNoCapture.append("current time of day/night")
					.append(" (off hours: '").append(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_CAPTURE_SCHEDULE_OFF_HOURS)).append("'.");
			isAudioCaptureDisabledRightNow = true;

		} else if (limitBasedOnCaptureSamplingRatio()) {
			msgNoCapture.append("a sampling ratio definition. ")
					.append("Ratio is '").append(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_SAMPLING_RATIO)).append("'. ")
					.append("Currently on iteration ").append(this.samplingRatioIteration).append(" of ").append(this.samplingRatioArr[0] + this.samplingRatioArr[1]).append(".");
			isAudioCaptureDisabledRightNow = true;

		} else if (!app.isGuardianRegistered()) {
			msgNoCapture.append("the Guardian not having been activated/registered.");
			isAudioCaptureDisabledRightNow = true;

		}

		if (isAudioCaptureDisabledRightNow && printFeedbackInLog) {
			Log.d(logTag, msgNoCapture
					.insert(0, DateTimeUtils.getDateTime() + " - AudioCapture paused due to ")
					.append(" Waiting ").append(app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION)).append(" seconds before next attempt.")
					.toString());
		}

		return isAudioCaptureDisabledRightNow;
	}

	public boolean updateCaptureQueue(long timeStampFile, long timeStampActual, int sampleRate) {

		queueCaptureTimestamp_File[0] = queueCaptureTimestamp_File[1];
		queueCaptureTimestamp_File[1] = timeStampFile;

		queueCaptureTimestamp_Actual[0] = queueCaptureTimestamp_Actual[1];
		queueCaptureTimestamp_Actual[1] = timeStampActual;

		queueCaptureSampleRate[0] = queueCaptureSampleRate[1];
		queueCaptureSampleRate[1] = sampleRate;

		return (queueCaptureTimestamp_File[0] > 0);
	}

	public static String getCaptureFilePath(String captureDir, long timestamp, String fileExtension) {
		return captureDir + "/" + timestamp + "." + fileExtension;
	}

	public static boolean reLocateAudioCaptureFile(Context context, boolean isToBeEncoded, boolean isToBeClassified, long timestampFile, long timestampActual, int sampleRate, String fileExt) {
		boolean isEncodeFileMoved = true;
		boolean isClassifyFileMoved = true;
		File captureFile = new File(getCaptureFilePath(RfcxAudioFileUtils.audioCaptureDir(context),timestampFile,fileExt));
		if (captureFile.exists()) {
			try {

				if (isToBeEncoded) {
					File preEncodeFile = new File(RfcxAudioFileUtils.getAudioFileLocation_PreEncode(context, timestampActual, fileExt, sampleRate, "g10"));
					FileUtils.initializeDirectoryRecursively(RfcxAudioFileUtils.audioEncodeDir(context), false);
					WavUtils.copyWavFile(captureFile.getAbsolutePath(), preEncodeFile.getAbsolutePath(), sampleRate);
					isEncodeFileMoved = preEncodeFile.exists();
				}

				if (isToBeClassified) {
					File preClassifyFile = new File(RfcxAudioFileUtils.getAudioFileLocation_PreClassify(context, timestampActual, fileExt, sampleRate, "g10"));
					FileUtils.initializeDirectoryRecursively(RfcxAudioFileUtils.audioClassifyDir(context), false);
					WavUtils.copyWavFile(captureFile.getAbsolutePath(), preClassifyFile.getAbsolutePath(), sampleRate);
					isClassifyFileMoved = preClassifyFile.exists();
				}

				if (isEncodeFileMoved && isClassifyFileMoved) {
					FileUtils.delete(captureFile);
				}

			} catch (IOException e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		return isEncodeFileMoved && isClassifyFileMoved;
	}


	public static File checkOrCreateReSampledWav(Context context, String purpose, String inputFilePath, long timestamp, String fileExt, int inputSampleRate, int outputSampleRate, double outputGain) {

		String outputFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreEncode(context, timestamp, fileExt, outputSampleRate, "g"+Math.round(outputGain*10));

		if (purpose.equalsIgnoreCase("classify")) {
			outputFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreClassify(context, timestamp, fileExt, outputSampleRate, "g"+Math.round(outputGain*10));
		}

		if (FileUtils.exists(outputFilePath)) {

			Log.d(logTag, "Already exists: "+ RfcxAssetCleanup.conciseFilePath(outputFilePath, RfcxGuardian.APP_ROLE));
			return (new File(outputFilePath));

		} else if (!FileUtils.exists(inputFilePath)) {

			Log.e(logTag, "Input file does not exist: "+ RfcxAssetCleanup.conciseFilePath(inputFilePath, RfcxGuardian.APP_ROLE));

		} else {

			try {

				WavUtils.resampleWavWithGain(inputFilePath, outputFilePath, inputSampleRate, outputSampleRate, outputGain);
				return (new File(outputFilePath));

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		return null;
	}

	public static int getAudioFileDurationInMilliseconds(File audioFileObj, int fallbackDuration, boolean printResultsToLog) {
		int audioDuration = fallbackDuration;

		try {
			long startTime = System.currentTimeMillis();
			MediaPlayer mediaPlayer = new MediaPlayer();
			FileInputStream fileInputStream = new FileInputStream(audioFileObj);
			mediaPlayer.setDataSource(fileInputStream.getFD());
			mediaPlayer.prepare();
			audioDuration = mediaPlayer.getDuration();
			mediaPlayer.release();
			fileInputStream.close();
			if (printResultsToLog) {
				Log.v(logTag, "Measured Audio Duration: "+audioDuration+" ms, "
						+ RfcxAssetCleanup.conciseFilePath(audioFileObj.getAbsolutePath(), RfcxGuardian.APP_ROLE)
						+ " (" + (System.currentTimeMillis() - startTime) + " ms to process the measurement)"
				);
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return audioDuration;
	}



	public Pair<byte[], Integer> getAudioBuffer() {
		if (wavRecorderForCompanion == null) return null;
		return wavRecorderForCompanion.getAudioBuffer();
	}

	public Boolean isAudioChanged() {
		return wavRecorderForCompanion.isAudioChanged();
	}



	public static void cleanupCaptureDirectory(Context context, long maxAgeInMilliseconds) {

		ArrayList<String> excludeFromCleanup = new ArrayList<String>();

		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup( new String[]{ RfcxAudioFileUtils.audioCaptureDir(context) }, excludeFromCleanup, Math.round(maxAgeInMilliseconds/60000), false, false );
	}

}
