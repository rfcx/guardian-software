package org.rfcx.guardian.audio.encode;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.audio.capture.AudioCaptureUtils;
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

public class AudioEncodeQueueIntentService extends IntentService {
	
	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioEncodeQueueIntentService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "AudioEncodeQueue";
		
	public AudioEncodeQueueIntentService() {
		super(logTag);
	}

	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;

		RfcxGuardian app = (RfcxGuardian) getApplication();
		Context context = app.getApplicationContext();
		
		long[] captureTimeStampQueue = app.audioCaptureUtils.captureTimeStampQueue;
		
		try {
		
			long captureLoopPeriod = (long) app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
			int encodingBitRate = app.rfcxPrefs.getPrefAsInt("audio_encode_bitrate");
			int audioSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate");
			String encodeCodec = app.rfcxPrefs.getPrefAsString("audio_encode_codec");
			String captureFileExtension = "wav";
			
			if (AudioCaptureUtils.reLocateAudioCaptureFile(context, captureTimeStampQueue[0], captureFileExtension)) {
				
				String preEncodeFilePath = RfcxAudio.getAudioFileLocation_PreEncode(context, captureTimeStampQueue[0],captureFileExtension);	
				
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
				
//				if (queuedEncodingJobUri == null) {
//					Log.e(logTag,"Failed to trigger AudioEncode via ContentProvider...");
//					if ((new File(preEncodeFilePath)).exists()) { (new File(preEncodeFilePath)).delete(); }
//				}
				
			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	
	}

}
