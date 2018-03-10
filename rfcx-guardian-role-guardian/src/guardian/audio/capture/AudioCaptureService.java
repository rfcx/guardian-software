package guardian.audio.capture;

import java.io.IOException;

import rfcx.utility.misc.FileUtils;
import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import guardian.RfcxGuardian;

public class AudioCaptureService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioCaptureService.class);
	
	private static final String SERVICE_NAME = "AudioCapture";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioCaptureSvc audioCaptureSvc;
	
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
		return START_STICKY;
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
			boolean isWavRecorderInitialized = false;

			int audioSampleRate = 0;
			int audioCycleDuration = 0;
			long audioCycleQuarterDuration = 0;
			long captureTimeStamp = 0; // timestamp of beginning of audio clip
			
			try {
				
				while (audioCaptureService.runFlag) {
					
					try {
						
						if (app.audioCaptureUtils.isAudioCaptureAllowed()) {
							
							if (		(app.rfcxPrefs.getPrefAsInt("audio_sample_rate") != audioSampleRate)
								||	(app.rfcxPrefs.getPrefAsInt("audio_cycle_duration") != audioCycleDuration)
								) {
								
								audioSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate");
								audioCycleDuration = app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
								audioCycleQuarterDuration = (long) Math.round( audioCycleDuration / 4 );
								
								Log.d(logTag, (new StringBuilder())
											.append("Capture Params: ")
											.append(audioCycleDuration).append(" ms, ")
											.append(audioSampleRate).append(" Hz")
											.toString());
							}
						
							if (!isWavRecorderInitialized) {
								captureTimeStamp = System.currentTimeMillis();
								wavRecorder = AudioCaptureUtils.initializeWavRecorder(captureDir, captureTimeStamp, audioSampleRate);
								wavRecorder.startRecorder();
								isWavRecorderInitialized = true;
							} else {
								captureTimeStamp = System.currentTimeMillis();
								wavRecorder.swapOutputFile(AudioCaptureUtils.getCaptureFilePath(captureDir, captureTimeStamp, "wav"));
							}

							if (app.audioCaptureUtils.updateCaptureTimeStampQueue(captureTimeStamp)) {
								app.rfcxServiceHandler.triggerService("AudioQueueEncode", true);
							}
							
						}
						
						for (int loopQuarterIteration = 0; loopQuarterIteration < 4; loopQuarterIteration++) {
							app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
							Thread.sleep(audioCycleQuarterDuration);
						}
						
					} catch (Exception e) {
						RfcxLog.logExc(logTag, e);
						audioCaptureService.runFlag = false;
					}
				}
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				
			} finally {
				audioCaptureService.runFlag = false;
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
			
			Log.v(logTag, "Stopping service: "+logTag);
				
		}
	}
	
	
}
