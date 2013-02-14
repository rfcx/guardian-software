package org.rfcx.src_audio;

import org.rfcx.rfcx_src_android.RfcxSource;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioCaptureService extends Service {

	private static final String TAG = AudioCaptureService.class.getSimpleName();
	
	AudioStateAlt audioState = new AudioStateAlt();

	private boolean runFlag = false;
	private AudioCapture audioCapture;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.audioCapture = new AudioCapture();
		Log.d(TAG, "onCreated()");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		this.audioCapture.start();
		Log.d(TAG, "onStarted()");
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		this.audioCapture.interrupt();
		this.audioCapture = null;
		Log.d(TAG, "onDestroyed()");
	}

	private class AudioCapture extends Thread {

		public AudioCapture() {
			super("AudioCaptureService-AudioCapture");
		}

		@Override
		public void run() {
			AudioCaptureService audioCaptureService = AudioCaptureService.this;
			RfcxSource rfcxSource = (RfcxSource) getApplicationContext();
			try {
				int bufferSize = 4 * AudioRecord.getMinBufferSize(
						AudioStateAlt.CAPTURE_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
						AudioFormat.ENCODING_PCM_16BIT);
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, AudioStateAlt.CAPTURE_SAMPLE_RATE,
						AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
						bufferSize);
				short[] buffer = new short[AudioStateAlt.BUFFER_LENGTH];
				audioRecord.startRecording();
				Log.d(TAG, "AudioCaptureService started (buffer: "+bufferSize+")");
				
				while (audioCaptureService.runFlag) {
					try {
						int bufferReadResult = audioRecord.read(buffer, 0, AudioStateAlt.BUFFER_LENGTH);
//						for (int i = 0; i < AudioStateAlt.FFT_RESOLUTION*2 && i < bufferReadResult; i++) {
//							samples[i] = (double) buffer[i] / 32768.0;
//						}
						audioState.addFrame(buffer, rfcxSource);
					} catch (Exception e) {
						audioCaptureService.runFlag = false;
					}
				}
				Log.d(TAG, "Stopping "+TAG);
				audioRecord.stop();
			} catch (Exception e) {
				audioCaptureService.runFlag = false;
			}
		}
	}

}
