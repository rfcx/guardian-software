package org.rfcx.guardian.guardian.audio.capture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.asset.MetaSnapshotService;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class AudioCaptureService extends Service {

	public static final String SERVICE_NAME = "AudioCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCaptureService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioCaptureSvc audioCaptureSvc;

	private int audioCycleDuration = 0;
	private int audioSampleRate = 0;

	private long innerLoopIterationDuration = 0;
	private static final int innerLoopIterationCount = 4;
	
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

			app.audioCaptureUtils.queueCaptureTimeStamp = new long[] { 0, 0 };
			
			String captureDir = RfcxAudioFileUtils.audioCaptureDir(context);
			FileUtils.deleteDirectoryContents(captureDir);
			
			AudioCaptureWavRecorder wavRecorder = null;

			long captureTimeStamp = 0; // timestamp at beginning of capture loop
				
			while (audioCaptureService.runFlag) {
				
				try {

					boolean isAudioCaptureDisabled = app.audioCaptureUtils.isAudioCaptureDisabled(true);
					boolean isAudioCaptureAllowed = !isAudioCaptureDisabled && app.audioCaptureUtils.isAudioCaptureAllowed(true, true);

					if ( confirmOrSetAudioCaptureParameters() && !isAudioCaptureDisabled && isAudioCaptureAllowed ) {

						// in this case, we are starting the audio capture from a stopped/pre-initialized state
						captureTimeStamp = System.currentTimeMillis();
						wavRecorder = AudioCaptureUtils.initializeWavRecorder(captureDir, captureTimeStamp, audioSampleRate);
						wavRecorder.startRecorder();
							
					} else if (wavRecorder != null) {
						// in this case, we assume that the state has changed and capture is no longer allowed... 
						// ...but there is a capture in progress, so we take a snapshot and halt the recorder.
						captureTimeStamp = 0;
						wavRecorder.haltRecording();
						wavRecorder = null;
						Log.i(logTag, "Stopping audio capture.");
					}
						
					// queueing the last capture file (if there is one) for post-processing
					if (app.audioCaptureUtils.updateCaptureQueue(captureTimeStamp, audioSampleRate)) {
						app.rfcxServiceHandler.triggerIntentServiceImmediately( AudioQueuePostProcessingService.SERVICE_NAME);
					}

					// This ensures that the service registers as active more frequently than the capture loop duration
					for (int innerLoopIteration = 0; innerLoopIteration < innerLoopIterationCount; innerLoopIteration++) {
						app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
						Thread.sleep(innerLoopIterationDuration);
					}

					// Triggering creation of a metadata snapshot.
					// This is not directly related to audio capture, but putting it here ensures that snapshots will...
					// ...continue to be taken, whether or not CheckIns are actually being sent or whether audio is being captured.
					app.rfcxServiceHandler.triggerIntentServiceImmediately( MetaSnapshotService.SERVICE_NAME);
					
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

			app.audioCaptureUtils.updateSamplingRatioIteration();

			int prefsAudioCaptureSampleRate = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CAPTURE_SAMPLE_RATE);
			int prefsAudioCycleDuration = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION);
			
			if (	(this.audioSampleRate != prefsAudioCaptureSampleRate)
				||	(this.audioCycleDuration != prefsAudioCycleDuration)
				) {

				this.audioSampleRate = prefsAudioCaptureSampleRate;
				this.audioCycleDuration = prefsAudioCycleDuration;
				innerLoopIterationDuration = Math.round( (prefsAudioCycleDuration * 1000) / innerLoopIterationCount );
				
				Log.d(logTag, "Audio Capture Params: " + prefsAudioCycleDuration + " seconds, " + prefsAudioCaptureSampleRate + " Hz");
			}
			
		} else {
			return false;
		}
		
		return true;
	}


	
}
