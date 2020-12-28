package org.rfcx.guardian.guardian.audio.encode;


import org.rfcx.guardian.utility.asset.RfcxAudioUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureUtils;

public class AudioQueueEncodeService extends IntentService {

	private static final String SERVICE_NAME = "AudioQueueEncode";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioQueueEncodeService");
		
	public AudioQueueEncodeService() {
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

				String preEncodeFilePath = RfcxAudioUtils.getAudioFileLocation_PreEncode(context, queueCaptureTimeStamp[0], captureFileExtension);

				if (app.rfcxPrefs.getPrefAsBoolean("enable_audio_stream")) {

					int streamSampleRate = app.rfcxPrefs.getPrefAsInt("audio_stream_sample_rate");
					String streamCodec = app.rfcxPrefs.getPrefAsString("audio_stream_codec");
					int streamBitrate = app.rfcxPrefs.getPrefAsInt("audio_stream_bitrate");

					app.audioEncodeDb.dbEncodeQueue.insert(
						""+queueCaptureTimeStamp[0], captureFileExtension, "-", streamSampleRate,
							streamBitrate, streamCodec, captureLoopPeriod, captureLoopPeriod, "stream", preEncodeFilePath );
				}

				if (app.rfcxPrefs.getPrefAsBoolean("enable_audio_vault")) {

					int vaultSampleRate = app.rfcxPrefs.getPrefAsInt("audio_vault_sample_rate");
					String vaultCodec = app.rfcxPrefs.getPrefAsString("audio_vault_codec");
					int vaultBitrate = app.rfcxPrefs.getPrefAsInt("audio_vault_bitrate");

					app.audioEncodeDb.dbEncodeQueue.insert(
							""+queueCaptureTimeStamp[0], captureFileExtension, "-", vaultSampleRate,
							vaultBitrate, vaultCodec, captureLoopPeriod, captureLoopPeriod, "vault", preEncodeFilePath );
				}
				
			} else {
				Log.e(logTag, "Queued audio file does not exist: "+RfcxAudioUtils.getAudioFileLocation_PreEncode(context, queueCaptureTimeStamp[0],captureFileExtension));
			}

			app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("AudioEncodeJob", 4 * app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000 );
				
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		}
		
	}
	
	
}
