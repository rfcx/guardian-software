package org.rfcx.guardian.guardian.audio.capture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class AudioCaptureService extends Service {

	private static final String SERVICE_NAME = "AudioCapture";

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
			
			String captureDir = RfcxAudioUtils.captureDir(context);
			FileUtils.deleteDirectoryContents(captureDir);
			
			AudioCaptureWavRecorder wavRecorder = null;

			long captureTimeStamp = 0; // timestamp at beginning of capture loop

			String[] recordedList = app.diagnosticDb.dbRecordedDiagnostic.getLatestRow();
			String[] syncedList = app.diagnosticDb.dbSyncedDiagnostic.getLatestRow();
			if (recordedList[0] == null && syncedList[0] == null) {
				app.diagnosticDb.dbRecordedDiagnostic.insert();
				app.diagnosticDb.dbSyncedDiagnostic.insert();
			}
				
			while (audioCaptureService.runFlag) {
				
				try {

					boolean isCaptureAllowed = app.audioCaptureUtils.isAudioCaptureAllowed(true);

					if (confirmOrSetAudioCaptureParameters() && isCaptureAllowed) {

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
						
					// queueing the last capture file for encoding, if there is one
					if (app.audioCaptureUtils.updateCaptureQueue(captureTimeStamp, audioSampleRate)) {
						app.rfcxServiceHandler.triggerIntentServiceImmediately("AudioQueueEncode");
					}

					// If capture is not allowed, we extend the capture cycle duration by a factor of AudioCaptureUtils.inReducedCaptureModeExtendCaptureCycleByFactorOf
					int currInnerLoopIterationCount = isCaptureAllowed ? innerLoopIterationCount : (AudioCaptureUtils.inReducedCaptureModeExtendCaptureCycleByFactorOf * innerLoopIterationCount);
					// This ensures that the service registers as active more frequently than the capture loop duration
					for (int innerLoopIteration = 0; innerLoopIteration < currInnerLoopIterationCount; innerLoopIteration++) {
						app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
						Thread.sleep(innerLoopIterationDuration);
					}

					// Triggering creation of a metadata snapshot.
					// This is not directly related to audio capture, but putting it here ensures that snapshots will...
					// ...continue to be taken, whether or not CheckIns are actually being sent or whether audio is being captured.
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
				innerLoopIterationDuration = (long) Math.round( (prefsAudioCycleDuration * 1000) / innerLoopIterationCount );
				
				Log.d(logTag, "Audio Capture Params: " + prefsAudioCycleDuration + " seconds, " + prefsAudioSampleRate + " Hz");
			}
			
		} else {
			return false;
		}
		
		return true;
	}

	
	
}
