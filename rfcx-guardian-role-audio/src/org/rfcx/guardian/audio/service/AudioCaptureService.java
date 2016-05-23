package org.rfcx.guardian.audio.service;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.audio.capture.AudioCapture;
import org.rfcx.guardian.audio.wav.WavAudioRecorder;
import org.rfcx.guardian.utility.audio.AudioFile;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

public class AudioCaptureService extends Service {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioCaptureService.class.getSimpleName();
	
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
		Log.v(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioCaptureSvc.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(TAG, e);
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

			app.audioCapture = new AudioCapture(context);
			app.audioCapture.captureTimeStampQueue = new long[] { 0, 0 };
			
			AudioFile.cleanupCaptureDirectory(context);
			AudioFile.cleanupEncodeDirectory(context);
			String captureDir = AudioFile.captureDir(context);

			
			long prefsCaptureLoopPeriod = (long) app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
			int prefsEncodingBitRate = app.rfcxPrefs.getPrefAsInt("audio_encode_bitrate");
			int prefsAudioSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate");
			boolean encodeOnCapture = app.rfcxPrefs.getPrefAsString("audio_encode_codec").equalsIgnoreCase("aac");
			String captureFileExtension = (encodeOnCapture) ? "m4a" : "wav";
			int prefsAudioBatteryCutoff = app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff");
			
			try {
				
				Log.d(TAG, "Capture Loop Period: "+ prefsCaptureLoopPeriod +"ms");
				
				while (audioCaptureService.runFlag) {
					try {
						if (app.audioCapture.isBatteryChargeSufficientForCapture()) {
							
							long timeStamp = System.currentTimeMillis();
							
							if (encodeOnCapture) {
								
								MediaRecorder recorder = AudioCapture.getAacRecorder(captureDir, timeStamp, captureFileExtension, prefsEncodingBitRate, prefsAudioSampleRate);
								recorder.start();
								if (app.audioCapture.updateCaptureTimeStampQueue(timeStamp)) { 
									app.rfcxServiceHandler.triggerIntentServiceImmediately("AudioEncodeTrigger");
								}
								Thread.sleep(prefsCaptureLoopPeriod);
								recorder.stop();
								recorder.release();
								
							} else {
								
								WavAudioRecorder recorder = AudioCapture.getWavRecorder(captureDir, timeStamp, captureFileExtension, prefsAudioSampleRate);
								recorder.start();
								if (app.audioCapture.updateCaptureTimeStampQueue(timeStamp)) { 
									app.rfcxServiceHandler.triggerIntentServiceImmediately("AudioEncodeTrigger");
								}
								Thread.sleep(prefsCaptureLoopPeriod);
								recorder.stop();
								recorder.release();
								
							}
							
						} else {
							Thread.sleep(2*prefsCaptureLoopPeriod);
							Log.i(TAG, "AudioCapture disabled due to low battery level"
									+" (current: "+app.deviceBattery.getBatteryChargePercentage(context, null)+"%, required: "+prefsAudioBatteryCutoff+"%)."
									+" Waiting "+(Math.round(2*prefsCaptureLoopPeriod/1000))+" seconds before next attempt.");
						}
					} catch (Exception e) {
						app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
						audioCaptureService.runFlag = false;
						RfcxLog.logExc(TAG, e);
					}
				}
				Log.v(TAG, "Stopping service: "+TAG);
				
			} catch (Exception e) {
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				audioCaptureService.runFlag = false;
				RfcxLog.logExc(TAG, e);
			}
		}
	}
	
	
}
