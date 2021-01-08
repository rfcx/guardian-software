package org.rfcx.guardian.guardian.audio.capture;


import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

import java.util.ArrayList;

public class AudioQueuePostProcessingService extends IntentService {

	private static final String SERVICE_NAME = "AudioQueuePostProcessing";

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
			
			long captureLoopPeriod = app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000 ;
			String captureFileExt = "wav";
			long captureTimeStamp = app.audioCaptureUtils.queueCaptureTimeStamp[0];
			int captureSampleRate = app.audioCaptureUtils.queueCaptureSampleRate[0];

			int jobCount_Encode = 0;
			int jobCount_Classify = 0;

			boolean isEnabled_audioStream = app.rfcxPrefs.getPrefAsBoolean("enable_audio_stream");
			boolean isEnabled_audioVault = app.rfcxPrefs.getPrefAsBoolean("enable_audio_vault");
			boolean isEnabled_audioClassify = app.rfcxPrefs.getPrefAsBoolean("enable_audio_classify");

			if (AudioCaptureUtils.reLocateAudioCaptureFile(context, (isEnabled_audioStream || isEnabled_audioVault), isEnabled_audioClassify, captureTimeStamp, captureSampleRate, captureFileExt)) {

				String preEncodeFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreEncode(context, captureTimeStamp, captureFileExt, captureSampleRate, null);
				String preClassifyFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreClassify(context, captureTimeStamp, captureFileExt, captureSampleRate, null);

				// Queue Encoding for Stream
				if (isEnabled_audioStream) {

					int streamSampleRate = app.rfcxPrefs.getPrefAsInt("audio_stream_sample_rate");
					String streamCodec = app.rfcxPrefs.getPrefAsString("audio_stream_codec");
					int streamBitrate = app.rfcxPrefs.getPrefAsInt("audio_stream_bitrate");

					jobCount_Encode += app.audioEncodeDb.dbQueued.insert(
						""+captureTimeStamp, captureFileExt, "-", streamSampleRate,
							streamBitrate, streamCodec, captureLoopPeriod, captureLoopPeriod, "stream", preEncodeFilePath, captureSampleRate );
				}



				// Queue Encoding for Vault
				if (isEnabled_audioVault) {

					int vaultSampleRate = app.rfcxPrefs.getPrefAsInt("audio_vault_sample_rate");
					String vaultCodec = app.rfcxPrefs.getPrefAsString("audio_vault_codec");
					int vaultBitrate = app.rfcxPrefs.getPrefAsInt("audio_vault_bitrate");

					jobCount_Encode += app.audioEncodeDb.dbQueued.insert(
							""+captureTimeStamp, captureFileExt, "-", vaultSampleRate,
							vaultBitrate, vaultCodec, captureLoopPeriod, captureLoopPeriod, "vault", preEncodeFilePath, captureSampleRate );
				}



				// Queue Classification
				if (isEnabled_audioClassify) {

					int classifierSampleRate = 12000;//app.rfcxPrefs.getPrefAsInt("audio_stream_sample_rate");

					jobCount_Classify += app.audioClassifyDb.dbQueued.insert(
							""+captureTimeStamp, captureFileExt, "-", classifierSampleRate,
							0, "-", captureLoopPeriod, captureLoopPeriod, "-", preClassifyFilePath, captureSampleRate
							);
				}


				
			} else {
				Log.e(logTag, "Failed to prepare captured audio for post processing.");
			}

			app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("AudioEncodeJob", 4 * captureLoopPeriod );
			app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("AudioClassifyJob", 4 * captureLoopPeriod );

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
