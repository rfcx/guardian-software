package org.rfcx.guardian.guardian.audio.capture;


import org.rfcx.guardian.guardian.audio.classify.AudioClassifyPrepareService;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeJobService;
import org.rfcx.guardian.utility.asset.RfcxAsset;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

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
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		
		app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
	
		try {
			
			long captureLoopPeriod = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) * 1000 ;
			String captureFileExt = "wav";
			long captureTimeStamp = app.audioCaptureUtils.queueCaptureTimeStamp[0];
			int captureSampleRate = app.audioCaptureUtils.queueCaptureSampleRate[0];

			int jobCount_Encode = 0;
			int jobCount_Classify = 0;

			boolean isEnabled_audioStream = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_STREAM);
			boolean isEnabled_audioVault = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_VAULT);
			boolean isEnabled_audioClassify = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_CLASSIFY);

			if (AudioCaptureUtils.reLocateAudioCaptureFile(context, (isEnabled_audioStream || isEnabled_audioVault), isEnabled_audioClassify, captureTimeStamp, captureSampleRate, captureFileExt)) {

				String preEncodeFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreEncode(context, captureTimeStamp, captureFileExt, captureSampleRate, null);
				String preClassifyFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreClassify(context, captureTimeStamp, captureFileExt, captureSampleRate, null);

				// Queue Encoding for Stream
				if (isEnabled_audioStream) {

					int streamSampleRate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_STREAM_SAMPLE_RATE);
					String streamCodec = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_STREAM_CODEC);
					int streamBitrate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_STREAM_BITRATE);

					jobCount_Encode += app.audioEncodeDb.dbQueued.insert(
						""+captureTimeStamp, captureFileExt, "-", streamSampleRate,
							streamBitrate, streamCodec, captureLoopPeriod, captureLoopPeriod, "stream", preEncodeFilePath, captureSampleRate );
				}



				// Queue Encoding for Vault
				if (isEnabled_audioVault) {

					int vaultSampleRate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_VAULT_SAMPLE_RATE);
					String vaultCodec = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_VAULT_CODEC);
					int vaultBitrate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_VAULT_BITRATE);

					jobCount_Encode += app.audioEncodeDb.dbQueued.insert(
							""+captureTimeStamp, captureFileExt, "-", vaultSampleRate,
							vaultBitrate, vaultCodec, captureLoopPeriod, captureLoopPeriod, "vault", preEncodeFilePath, captureSampleRate );
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
							String classifierFilePath = classiferRow[6];
							String classifierWindowSize = classiferRow[8];
							String classifierStepSize = classiferRow[9];
							String classifierClasses = classiferRow[10];

							jobCount_Classify += app.audioClassifyDb.dbQueued.insert(
									"" + captureTimeStamp, classifierId, classifierVersion,
											captureSampleRate, classifierSampleRate,
											preClassifyFilePath, classifierFilePath,
											classifierWindowSize, classifierStepSize, classifierClasses);
						}
					}
				}


				
			} else {
				Log.e(logTag, "Failed to prepare captured audio for post processing.");
			}

			app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut( AudioEncodeJobService.SERVICE_NAME, 4 * captureLoopPeriod );
			app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut( AudioClassifyPrepareService.SERVICE_NAME, 4 * captureLoopPeriod );

//			if ((jobCount_Encode+jobCount_Classify) == 0) {
//				// Scan Encode, Capture & Classify Directories for cleanup
//				RfcxAssetCleanup assetCleanup = new RfcxAssetCleanup(RfcxGuardian.APP_ROLE);
//				assetCleanup.runCleanupOnOneDirectory(RfcxAudioFileUtils.audioCaptureDir(context), 2 * captureLoopPeriod, false);
//				assetCleanup.runCleanupOnOneDirectory(RfcxAudioFileUtils.audioEncodeDir(context), 2 * captureLoopPeriod, false);
//				assetCleanup.runCleanupOnOneDirectory(RfcxAudioFileUtils.audioClassifyDir(context), 2 * captureLoopPeriod, false);
//			}
//
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME, false);
		}
		
	}
	
	
}
