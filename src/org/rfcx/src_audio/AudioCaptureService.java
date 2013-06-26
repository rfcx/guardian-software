package org.rfcx.src_audio;

import org.rfcx.src_android.RfcxSource;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioCaptureService extends Service {

	private static final String TAG = AudioCaptureService.class.getSimpleName();

	private boolean runFlag = false;
	private AudioCapture audioCapture;

	private RfcxSource app = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.audioCapture = new AudioCapture();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		app = (RfcxSource) getApplication();
		if (app.verboseLogging) Log.d(TAG, "Starting service: "+TAG);
		app.isServiceRunning_AudioCapture = true;
		this.audioCapture.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isServiceRunning_AudioCapture = false;
		this.audioCapture.interrupt();
		this.audioCapture = null;
	}

	private class AudioCapture extends Thread {

		public AudioCapture() {
			super("AudioCaptureService-AudioCapture");
		}

		@Override
		public void run() {
			AudioCaptureService audioCaptureService = AudioCaptureService.this;
			app = (RfcxSource) getApplicationContext();
			AudioState audioState = app.audioState;
			try {
				int bufferSize = 12 * AudioRecord.getMinBufferSize(
					AudioState.CAPTURE_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
				AudioRecord audioRecord = new AudioRecord(
					MediaRecorder.AudioSource.MIC, AudioState.CAPTURE_SAMPLE_RATE,
					AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
				short[] audioBuffer = new short[AudioState.BUFFER_LENGTH];
				audioRecord.startRecording();
				
				Thread.sleep(1000*AudioState.BUFFER_LENGTH/AudioState.CAPTURE_SAMPLE_RATE);
				
				while (audioCaptureService.runFlag) {
					try {
						audioRecord.read(audioBuffer, 0, AudioState.BUFFER_LENGTH);
						audioState.cachePcmBuffer(audioBuffer);
					} catch (Exception e) {
						Log.e(TAG, e.getMessage());
						audioCaptureService.runFlag = false;
						app.isServiceRunning_AudioCapture = false;
					}
				}
				if (app.verboseLogging) Log.d(TAG, "Stopping service: "+TAG);
				audioRecord.stop();
			} catch (InterruptedException e) {
				Log.e(TAG, "InterruptedException");
				audioCaptureService.runFlag = false;
				app.isServiceRunning_AudioCapture = false;
			} catch (Exception e) {
				e.printStackTrace();
				audioCaptureService.runFlag = false;
				app.isServiceRunning_AudioCapture = false;
			}
		}
	}

}
