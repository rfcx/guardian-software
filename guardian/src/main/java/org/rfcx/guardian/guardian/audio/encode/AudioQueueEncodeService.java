package org.rfcx.guardian.guardian.audio.encode;


import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
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

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioQueueEncodeService.class.getSimpleName());
		
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
			int encodingBitRate = app.rfcxPrefs.getPrefAsInt("audio_encode_bitrate");
			String encodingCodec = app.rfcxPrefs.getPrefAsString("audio_encode_codec");
			String captureFileExtension = "wav";

			long[] queueCaptureTimeStamp = app.audioCaptureUtils.queueCaptureTimeStamp;
			int[] queueCaptureSampleRate = app.audioCaptureUtils.queueCaptureSampleRate;
			
			if (AudioCaptureUtils.reLocateAudioCaptureFile(context, queueCaptureTimeStamp[0], captureFileExtension)) {
				
				String preEncodeFilePath = RfcxAudioUtils.getAudioFileLocation_PreEncode(context, queueCaptureTimeStamp[0],captureFileExtension);
				
				app.audioEncodeDb.dbEncodeQueue.insert(
						""+queueCaptureTimeStamp[0],
						captureFileExtension,
						"-",
						queueCaptureSampleRate[0],
						encodingBitRate,
						encodingCodec,
						captureLoopPeriod,
						captureLoopPeriod,
						preEncodeFilePath
						);
				
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
