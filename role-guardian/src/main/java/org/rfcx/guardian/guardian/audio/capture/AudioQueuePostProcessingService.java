package org.rfcx.guardian.guardian.audio.capture;


import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

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
			
			long captureLoopPeriod = (long) Math.round( app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") * 1000 );
			String captureFileExtension = "wav";

			long[] queueCaptureTimeStamp = app.audioCaptureUtils.queueCaptureTimeStamp;
			int[] queueCaptureSampleRate = app.audioCaptureUtils.queueCaptureSampleRate;
			
			if (AudioCaptureUtils.reLocateAudioCaptureFile(context, queueCaptureTimeStamp[0], captureFileExtension)) {

				String preEncodeFilePath = RfcxAudioFileUtils.getAudioFileLocation_PreEncode(context, queueCaptureTimeStamp[0], captureFileExtension);



				// Queue Audio Encode for Streaming
				if (app.rfcxPrefs.getPrefAsBoolean("enable_audio_stream")) {

					int streamSampleRate = app.rfcxPrefs.getPrefAsInt("audio_stream_sample_rate");
					String streamCodec = app.rfcxPrefs.getPrefAsString("audio_stream_codec");
					int streamBitrate = app.rfcxPrefs.getPrefAsInt("audio_stream_bitrate");

					app.audioEncodeDb.dbEncodeQueue.insert(
						""+queueCaptureTimeStamp[0], captureFileExtension, "-", streamSampleRate,
							streamBitrate, streamCodec, captureLoopPeriod, captureLoopPeriod, "stream", preEncodeFilePath, queueCaptureSampleRate[0] );
				}



				// Queue Audio Encode for the Vault
				if (app.rfcxPrefs.getPrefAsBoolean("enable_audio_vault")) {

					int vaultSampleRate = app.rfcxPrefs.getPrefAsInt("audio_vault_sample_rate");
					String vaultCodec = app.rfcxPrefs.getPrefAsString("audio_vault_codec");
					int vaultBitrate = app.rfcxPrefs.getPrefAsInt("audio_vault_bitrate");

					app.audioEncodeDb.dbEncodeQueue.insert(
							""+queueCaptureTimeStamp[0], captureFileExtension, "-", vaultSampleRate,
							vaultBitrate, vaultCodec, captureLoopPeriod, captureLoopPeriod, "vault", preEncodeFilePath, queueCaptureSampleRate[0] );
				}



				// Queue Audio Classification
				if (app.rfcxPrefs.getPrefAsBoolean("enable_audio_classify")) {

					int classifierSampleRate = app.rfcxPrefs.getPrefAsInt("audio_stream_sample_rate");

//					app.audioEncodeDb.dbEncodeQueue.insert(
//							""+queueCaptureTimeStamp[0], captureFileExtension, "-", classifierSampleRate,
//							vaultBitrate, vaultCodec, captureLoopPeriod, captureLoopPeriod, "vault", preEncodeFilePath, queueCaptureSampleRate[0] );
				}


				
			} else {
				Log.e(logTag, "Queued audio file does not exist: "+ RfcxAudioFileUtils.getAudioFileLocation_PreEncode(context, queueCaptureTimeStamp[0],captureFileExtension));
			}

			long serviceTimeOutDuration = 4 * app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;
			app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("AudioEncodeJob", serviceTimeOutDuration );
			app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("AudioClassifyJob", serviceTimeOutDuration );
				
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME, false);
		}
		
	}
	
	
}
