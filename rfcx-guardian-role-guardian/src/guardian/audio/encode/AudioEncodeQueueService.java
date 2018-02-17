package guardian.audio.encode;

import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import guardian.RfcxGuardian;
import guardian.audio.capture.AudioCaptureUtils;

public class AudioEncodeQueueService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioEncodeQueueService.class);
	
	private static final String SERVICE_NAME = "AudioEncodeQueue";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioEncodeQueue audioEncodeQueue;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioEncodeQueue = new AudioEncodeQueue();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioEncodeQueue.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.audioEncodeQueue.interrupt();
		this.audioEncodeQueue = null;
	}
	
	private class AudioEncodeQueue extends Thread {

		public AudioEncodeQueue() {
			super("AudioEncodeQueueService-AudioEncodeQueue");
		}
		
		@Override
		public void run() {
			AudioEncodeQueueService audioEncodeQueueInstance = AudioEncodeQueueService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
				
			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
		
			try {
				
				long captureLoopPeriod = (long) app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
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

				app.rfcxServiceHandler.triggerService("AudioEncodeJob", false);
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				
			} finally {
				audioEncodeQueueInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}

		}
	}
	

}
