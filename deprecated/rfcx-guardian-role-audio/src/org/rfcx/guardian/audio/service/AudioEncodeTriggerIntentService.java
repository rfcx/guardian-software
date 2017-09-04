package org.rfcx.guardian.audio.service;

import java.io.File;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.audio.utils.AudioCaptureUtils;
import org.rfcx.guardian.utility.audio.RfcxAudio;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class AudioEncodeTriggerIntentService extends IntentService {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioEncodeTriggerIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "AudioEncodeTrigger";
		
	public AudioEncodeTriggerIntentService() {
		super(logTag);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;

		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		
		long[] captureTimeStampQueue = app.audioCapture.captureTimeStampQueue;
		
		try {
		
			long captureLoopPeriod = (long) app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
			int encodingBitRate = app.rfcxPrefs.getPrefAsInt("audio_encode_bitrate");
			int audioSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate");
			String encodeCodec = app.rfcxPrefs.getPrefAsString("audio_encode_codec");
			boolean encodeOnCapture = encodeCodec.equalsIgnoreCase("aac");
			String captureFileExtension = (encodeOnCapture) ? "m4a" : "wav";
			
			if (AudioCaptureUtils.reLocateAudioCaptureFile(context, captureTimeStampQueue[0], captureFileExtension)) {
				
				String preEncodeFilePath = RfcxAudio.getAudioFileLocation_PreEncode(captureTimeStampQueue[0],captureFileExtension);
				
				ContentValues capturedAudioValues = new ContentValues();
				capturedAudioValues.put("created_at", "-");
				capturedAudioValues.put("timestamp", captureTimeStampQueue[0]);
				capturedAudioValues.put("format", captureFileExtension);
				capturedAudioValues.put("digest", "-");
				capturedAudioValues.put("filepath", preEncodeFilePath);
				capturedAudioValues.put("samplerate", audioSampleRate);
				capturedAudioValues.put("bitrate", encodingBitRate);
				capturedAudioValues.put("codec", encodeCodec);
				capturedAudioValues.put("duration", captureLoopPeriod);
				capturedAudioValues.put("encode_duration", captureLoopPeriod);
	
				Uri queuedEncodingJobUri = getContentResolver().insert( Uri.parse(RfcxRole.ContentProvider.encode.URI_QUEUE), capturedAudioValues );
				
				if (queuedEncodingJobUri == null) {
					Log.e(logTag,"Failed to trigger AudioEncode via ContentProvider...");
					if ((new File(preEncodeFilePath)).exists()) { (new File(preEncodeFilePath)).delete(); }
				}
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	
	}

}
