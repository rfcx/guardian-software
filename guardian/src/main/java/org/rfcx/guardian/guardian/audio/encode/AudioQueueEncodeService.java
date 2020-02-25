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
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioQueueEncodeService.class);
	
	private static final String SERVICE_NAME = "AudioQueueEncode";
		
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
			int audioSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate");
			String encodeCodec = app.rfcxPrefs.getPrefAsString("audio_encode_codec");
			String captureFileExtension = "wav";

			long[] captureTimeStampQueue = app.audioCaptureUtils.captureTimeStampQueue;
			
			if (AudioCaptureUtils.reLocateAudioCaptureFile(context, captureTimeStampQueue[0], captureFileExtension)) {
				
				String preEncodeFilePath = RfcxAudioUtils.getAudioFileLocation_PreEncode(context, captureTimeStampQueue[0],captureFileExtension);	
				
				app.audioEncodeDb.dbEncodeQueue.insert(
						""+captureTimeStampQueue[0],
						captureFileExtension,
						"-",
						audioSampleRate,
						encodingBitRate,
						encodeCodec,
						captureLoopPeriod,
						captureLoopPeriod,
						preEncodeFilePath
						);
				
			} else {
				Log.e(logTag, "Queued audio file does not exist: "+RfcxAudioUtils.getAudioFileLocation_PreEncode(context, captureTimeStampQueue[0],captureFileExtension));
			}

		//	app.rfcxServiceHandler.triggerService("AudioEncodeJob", false);
			app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("AudioEncodeJob", 4 * app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000 );
				
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			
		} finally {
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			app.rfcxServiceHandler.stopService(SERVICE_NAME);
		}
		
	}
	
	
}
