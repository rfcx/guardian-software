package org.rfcx.guardian.guardian.audio.capture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.asset.meta.MetaSnapshotService;
import org.rfcx.guardian.guardian.status.StatusCacheService;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

public class AudioCaptureService extends Service {

	public static final String SERVICE_NAME = "AudioCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCaptureService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioCaptureSvc audioCaptureSvc;

	private int audioCycleDuration = 0;
	private int audioSampleRate = 0;

	private long innerLoopIterationDuration = 0;
//	private static final int innerLoopIterationCount = 5;
	
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
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
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
		app.rfcxSvc.setRunState(SERVICE_NAME, false);
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

			app.rfcxSvc.reportAsActive(SERVICE_NAME);

			app.audioCaptureUtils.queueCaptureTimestamp_File = new long[] { 0, 0 };
			
			String captureDir = RfcxAudioFileUtils.audioCaptureDir(context);
			FileUtils.deleteDirectoryContents(captureDir);
			
			AudioCaptureWavRecorder wavRecorder = null;
				
			while (audioCaptureService.runFlag) {
				
				try {

					long captureTimestampFile = 0;
					long captureTimestampActual = 0;

					boolean isAudioCaptureEnabled = app.rfcxStatus.getLocalStatus( RfcxStatus.Group.AUDIO_CAPTURE, RfcxStatus.Type.ENABLED, true);

					boolean isAudioCaptureAllowed = isAudioCaptureEnabled
													&& app.rfcxStatus.getLocalStatus( RfcxStatus.Group.AUDIO_CAPTURE, RfcxStatus.Type.ALLOWED, true)
													&& app.rfcxStatus.getFetchedStatus( RfcxStatus.Group.AUDIO_CAPTURE, RfcxStatus.Type.ALLOWED);

					if ( confirmOrSetAudioCaptureParameters() && isAudioCaptureEnabled && isAudioCaptureAllowed ) {

						// in this case, we are starting the audio capture from a stopped/pre-initialized state
						captureTimestampFile = System.currentTimeMillis();
						wavRecorder = AudioCaptureUtils.initializeWavRecorder(captureDir, captureTimestampFile, audioSampleRate);
						wavRecorder.startRecorder();
						captureTimestampActual = System.currentTimeMillis();
						Log.i(logTag, "Start of Audio Capture Loop...");
							
					} else if (wavRecorder != null) {
						// in this case, we assume that the state has changed and capture is no longer allowed... 
						// ...but there is a capture in progress, so we take a snapshot and halt the recorder.
						wavRecorder.haltRecording();
						wavRecorder = null;
						Log.i(logTag, "Halting Audio Capture...");
					}

					// queueing the last capture file (if there is one) for post-processing
					if (app.audioCaptureUtils.updateCaptureQueue(captureTimestampFile, captureTimestampActual, audioSampleRate)) {
						app.rfcxSvc.triggerIntentServiceImmediately( AudioQueuePostProcessingService.SERVICE_NAME);
					}

					// Triggering creation of a metadata snapshot.
					// This is not directly related to audio capture, but putting it here ensures that snapshots will...
					// ...continue to be taken, whether or not CheckIns are actually being sent or whether audio is being captured.
					app.rfcxSvc.triggerIntentServiceImmediately( MetaSnapshotService.SERVICE_NAME);

					// This ensures that the service registers as active more frequently than the capture loop duration
					for (int innerLoopIteration = 1; innerLoopIteration <= RfcxStatus.ratioExpirationToAudioCycleDuration; innerLoopIteration++) {
						app.rfcxSvc.reportAsActive(SERVICE_NAME);
						if ((innerLoopIteration != RfcxStatus.ratioExpirationToAudioCycleDuration) || (captureTimestampActual == 0)) {
							Thread.sleep( innerLoopIterationDuration );
						} else {
							app.rfcxSvc.triggerIntentServiceImmediately( StatusCacheService.SERVICE_NAME );
							Thread.sleep( ( audioCycleDuration * 1000 ) - ( System.currentTimeMillis() - captureTimestampActual ) );
						}
					}

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxSvc.setRunState(SERVICE_NAME, false);
					audioCaptureService.runFlag = false;
				}
			}
			
			app.rfcxSvc.setRunState(SERVICE_NAME, false);
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
				innerLoopIterationDuration = Math.round( (prefsAudioCycleDuration * 1000) / RfcxStatus.ratioExpirationToAudioCycleDuration );
				
				Log.d(logTag, "Audio Capture Params"
						+ " - Cycle: " + DateTimeUtils.milliSecondDurationAsReadableString(prefsAudioCycleDuration*1000)
						+ " - Sample Rate: " + Math.round(((double) prefsAudioCaptureSampleRate)/1000) + " kHz");
			}
			
		} else {
			return false;
		}
		
		return true;
	}


	
}
