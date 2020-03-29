package org.rfcx.guardian.guardian.audio.capture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class AudioCaptureService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioCaptureService.class);
	
	private static final String SERVICE_NAME = "AudioCapture";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioCaptureSvc audioCaptureSvc;

	private int audioCycleDuration = 0; 
	private long loopQuarterDuration = 0;
	private int audioSampleRate = 0;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.audioCaptureSvc = new AudioCaptureSvc();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioCaptureSvc.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.audioCaptureSvc.interrupt();
		this.audioCaptureSvc = null;
	}

	private class AudioCaptureSvc extends Thread {

		public AudioCaptureSvc() {
			super("AudioCaptureService-AudioCaptureSvc");
		}

		@Override
		public void run() {
			AudioCaptureService audioCaptureService = AudioCaptureService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

			app.audioCaptureUtils.captureTimeStampQueue = new long[] { 0, 0 };
			
			String captureDir = RfcxAudioUtils.captureDir(context);
			FileUtils.deleteDirectoryContents(captureDir);
			
			AudioCaptureWavRecorder wavRecorder = null;

			long captureTimeStamp = 0; // timestamp of beginning of audio clip
				
			while (audioCaptureService.runFlag) {
				
				try {
					
					if (confirmOrSetAudioCaptureParameters()
						&& 	app.audioCaptureUtils.isAudioCaptureAllowed(app.rfcxPrefs.getPrefAsBoolean("verbose_logging"))
						) {
							
//						if (wavRecorder == null) {
							// in this case, we are starting the audio capture from a stopped/pre-initialized state
							captureTimeStamp = System.currentTimeMillis();
							wavRecorder = AudioCaptureUtils.initializeWavRecorder(captureDir, captureTimeStamp, audioSampleRate);
							wavRecorder.startRecorder();
// This line is the problem of get audio corrupted for encoding
//						} else {
//							// in this case, we are just taking a snapshot and moving capture output to a new file
//							// ( !!! THIS STILL NEEDS TO BE OPTIMIZED TO AVOID CAPTURE DOWNTIME !!! )
//							// Look in AudioCaptureWavRecorder for optimization...
//							captureTimeStamp = System.currentTimeMillis();
//							Log.d(logTag, "wavRecoder is not null");
//							wavRecorder.swapOutputFile(AudioCaptureUtils.getCaptureFilePath(captureDir, captureTimeStamp, "wav"));
//						}
							
					} else if (wavRecorder != null) {
						// in this case, we assume that the state has changed and capture is no longer allowed... 
						// ...but there is a capture in progress, so we take a snapshot and halt the recorder.
						captureTimeStamp = 0;
						Log.d(logTag, "halt wavRecorder");
						wavRecorder.haltRecording();
						wavRecorder = null;
						Log.i(logTag, "Stopping audio capture.");
					}
						
					// queueing the last capture file for encoding, if there is one
					if (app.audioCaptureUtils.updateCaptureTimeStampQueue(captureTimeStamp)) {
						app.rfcxServiceHandler.triggerIntentServiceImmediately("AudioQueueEncode");
					}

					for (int loopQuarterIteration = 0; loopQuarterIteration < 4; loopQuarterIteration++) {
						app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
						Thread.sleep(loopQuarterDuration);
					}

					// Triggering creation of a metadata snapshot, for retrieval during CheckIn.
					// This is unrelated to audio capture, but putting it here ensures that snapshots
					// ...will continue to be taken, whether or not checkins are actually being sent or audio is allowed to be captured.
					app.rfcxServiceHandler.triggerIntentServiceImmediately("ApiCheckInMetaSnapshot");
					
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					audioCaptureService.runFlag = false;
				}
			}
			
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			audioCaptureService.runFlag = false;
			
			Log.v(logTag, "Stopping service: "+logTag);
				
		}
	}
	
	private boolean confirmOrSetAudioCaptureParameters() {
		
		if (app != null) {

			int prefsAudioSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate");
			int prefsAudioCycleDuration = app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
			
			if (	(this.audioSampleRate != prefsAudioSampleRate)
				||	(this.audioCycleDuration != prefsAudioCycleDuration)
				) {

				this.audioSampleRate = prefsAudioSampleRate;
				this.audioCycleDuration = prefsAudioCycleDuration;
				loopQuarterDuration = (long) Math.round( (prefsAudioCycleDuration * 1000) / 4 );
				
				Log.d(logTag, (new StringBuilder())
						.append("Audio Capture Params: ")
						.append(prefsAudioCycleDuration).append(" seconds, ")
						.append(Math.round(prefsAudioSampleRate/1000)).append(" kHz").toString());
			}
			
		} else {
			return false;
		}
		
		return true;
	}
	
	
}
