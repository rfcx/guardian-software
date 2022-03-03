package org.rfcx.guardian.guardian.audio.capture;


import org.rfcx.guardian.guardian.audio.classify.AudioClassifyPrepareService;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeJobService;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

import java.util.List;

public class AudioQueuePostProcessingService extends IntentService {

	public static final String SERVICE_NAME = "AudioQueuePostProcessing";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioQueuePostProcessingService");

	public AudioQueuePostProcessingService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		
		app.rfcxSvc.reportAsActive(SERVICE_NAME);
	
		try {
			
			long captureLoopPeriod = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) * 1000 ;
			String captureFileExt = "wav";
			long captureTimestampFile = app.audioCaptureUtils.queueCaptureTimestamp_File[0];
			long captureTimestampActual = app.audioCaptureUtils.queueCaptureTimestamp_Actual[0];
			int captureSampleRate = app.audioCaptureUtils.queueCaptureSampleRate[0];
			double captureGain = 1.0;

			int jobCount_Encode = 0;
			int jobCount_Classify = 0;

			boolean isEnabled_audioStream = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_STREAM);
			boolean isEnabled_audioVault = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_VAULT);
			boolean isEnabled_audioClassify = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_CLASSIFY);

			if (AudioCaptureUtils.reLocateAudioCaptureFile(context, (isEnabled_audioStream || isEnabled_audioVault), isEnabled_audioClassify, captureTimestampFile, captureTimestampActual, captureSampleRate, captureFileExt)) {

				String preEncodeFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreEncode(context, captureTimestampActual, captureFileExt, captureSampleRate, "g"+Math.round(captureGain*10));
				String preClassifyFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreClassify(context, captureTimestampActual, captureFileExt, captureSampleRate, "g"+Math.round(captureGain*10));

				// Queue Encoding for Stream
				if (isEnabled_audioStream) {

					int streamSampleRate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_STREAM_SAMPLE_RATE);
					String streamCodec = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_STREAM_CODEC);
					int streamBitrate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_STREAM_BITRATE);

					if (streamSampleRate <= captureSampleRate) {
						jobCount_Encode += app.audioEncodeDb.dbQueued.insert(
								"" + captureTimestampActual, captureFileExt, "-", streamSampleRate,
								streamBitrate, streamCodec, captureLoopPeriod, captureLoopPeriod, "stream", preEncodeFilePath, captureSampleRate);
					} else {
						Log.e(logTag, "Stream encoding job skipped because Stream Sample Rate (" + streamSampleRate + ") is higher than Capture Sample Rate (" + captureSampleRate + ").");
					}
				}



				// Queue Encoding for Vault
				if (isEnabled_audioVault) {

					int vaultSampleRate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_VAULT_SAMPLE_RATE);
					String vaultCodec = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_VAULT_CODEC);
					int vaultBitrate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_VAULT_BITRATE);

					if (vaultSampleRate <= captureSampleRate) {
						jobCount_Encode += app.audioEncodeDb.dbQueued.insert(
								"" + captureTimestampActual, captureFileExt, "-", vaultSampleRate,
								vaultBitrate, vaultCodec, captureLoopPeriod, captureLoopPeriod, "vault", preEncodeFilePath, captureSampleRate);
					} else {
						Log.e(logTag, "Vault encoding job skipped because Vault Sample Rate (" + vaultSampleRate + ") is higher than Capture Sample Rate (" + captureSampleRate + ").");
					}
				}



				// Queue Classification
				if (isEnabled_audioClassify) {

					List<String[]> allActiveClassifiers = app.audioClassifierDb.dbActive.getAllRows();
					if (allActiveClassifiers.size() == 0) { Log.d(logTag, "There are currently no active classifiers."); }

					for (String[] classiferRow : allActiveClassifiers) {
						if (classiferRow[0] != null) {

							String classifierId = classiferRow[1];
							String classifierGuid = classiferRow[2];
							String classifierVersion = classiferRow[3];
							int classifierSampleRate = Integer.parseInt(classiferRow[7]);
							double classifierInputGain = Double.parseDouble(classiferRow[8]);
							String classifierFilePath = classiferRow[6];
							String classifierWindowSize = classiferRow[9];
							String classifierStepSize = classiferRow[10];
							String classifierClasses = classiferRow[11];

							if (app.audioClassifyUtils.isClassifyAllowedAtThisTimeOfDay(classifierId)) {
								if (classifierSampleRate <= captureSampleRate) {
									jobCount_Classify += app.audioClassifyDb.dbQueued.insert(
											"" + captureTimestampActual, classifierId, classifierVersion,
											captureSampleRate, classifierSampleRate, classifierInputGain,
											preClassifyFilePath, classifierFilePath,
											classifierWindowSize, classifierStepSize, classifierClasses);
								} else {
									Log.e(logTag, "Classification job skipped because Classifier Sample Rate (" + classifierSampleRate + ") is higher than Capture Sample Rate (" + captureSampleRate + ").");
								}
							}
						}
					}
				}

			} else {
				Log.e(logTag, "Failed to prepare captured audio for post processing.");
			}

			app.rfcxSvc.triggerOrForceReTriggerIfTimedOut( AudioClassifyPrepareService.SERVICE_NAME, 4 * captureLoopPeriod );
			app.rfcxSvc.triggerOrForceReTriggerIfTimedOut( AudioEncodeJobService.SERVICE_NAME, 4 * captureLoopPeriod );

			AudioCaptureUtils.cleanupCaptureDirectory(context, Math.round( RfcxAssetCleanup.DEFAULT_AUDIO_CYCLE_CLEANUP_BUFFER * captureLoopPeriod ));

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			app.rfcxSvc.stopService(SERVICE_NAME, false);
		}
		
	}
	
	
}
