package org.rfcx.guardian.audio.capture;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.audio.RfcxAudio;
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

//			app.audioCaptureUtils = new AudioCaptureUtils(context);
			app.audioCaptureUtils.captureTimeStampQueue = new long[] { 0, 0 };
			
			String captureDir = RfcxAudio.captureDir(context);
			FileUtils.deleteDirectoryContents(captureDir);

			long prefsCaptureLoopPeriod = (long) app.rfcxPrefs.getPrefAsInt("audio_cycle_duration");
			int prefsEncodingBitRate = app.rfcxPrefs.getPrefAsInt("audio_encode_bitrate");
			int prefsAudioSampleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate");
			int prefsAudioBatteryCutoff = app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff");
			//int prefsAudioScheduleOffHours = app.rfcxPrefs.getPrefAsInt("audio_schedule_off_hours");

			long captureTimeStamp;
			AudioCaptureWavRecorder wavRecorder_A;
			boolean isBatteryChargeSufficientForCapture = app.audioCaptureUtils.isBatteryChargeSufficientForCapture();
			boolean isCaptureAllowedAtThisTimeOfDay = app.audioCaptureUtils.isCaptureAllowedAtThisTimeOfDay();
		
			try {
				
				Log.d(TAG, "Capture Loop Period: "+ prefsCaptureLoopPeriod +"ms");
				
				while (audioCaptureService.runFlag) {
					try {
						
						if (isBatteryChargeSufficientForCapture && isCaptureAllowedAtThisTimeOfDay) {
							
							// set timestamp of beginning of audio clip
							captureTimeStamp = System.currentTimeMillis();
							
							// initialize and start audio capture
							wavRecorder_A = AudioCaptureUtils.getWavRecorder(captureDir, captureTimeStamp, "wav", prefsAudioSampleRate);
							wavRecorder_A.start();
							
							if (app.audioCaptureUtils.updateCaptureTimeStampQueue(captureTimeStamp)) { 
								app.rfcxServiceHandler.triggerIntentServiceImmediately("AudioEncodeQueue");
							}
							
							// sleep for intended length of capture clip
							Thread.sleep(prefsCaptureLoopPeriod);
							
							// cache state of whether capture is allowed (to be used on next capture loop)
							isBatteryChargeSufficientForCapture = app.audioCaptureUtils.isBatteryChargeSufficientForCapture();
							isCaptureAllowedAtThisTimeOfDay = app.audioCaptureUtils.isCaptureAllowedAtThisTimeOfDay();
							
							// stop and release recorder
							wavRecorder_A.stop();
							wavRecorder_A.release();
							
						} else {
							
							isBatteryChargeSufficientForCapture = app.audioCaptureUtils.isBatteryChargeSufficientForCapture();
							isCaptureAllowedAtThisTimeOfDay = app.audioCaptureUtils.isCaptureAllowedAtThisTimeOfDay();
							
							if (!isBatteryChargeSufficientForCapture) {
								
								Log.i(TAG, "AudioCapture disabled due to low battery level"
										+" (current: "+app.deviceBattery.getBatteryChargePercentage(context, null)+"%, required: "+prefsAudioBatteryCutoff+"%)."
										+" Waiting "+(Math.round(2*prefsCaptureLoopPeriod/1000))+" seconds before next attempt.");
								Thread.sleep(prefsCaptureLoopPeriod);
								
							} else if (!isCaptureAllowedAtThisTimeOfDay) {
								
								Log.i(TAG, "AudioCapture disabled during specified off hours..");
							}
							Thread.sleep(prefsCaptureLoopPeriod);
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
