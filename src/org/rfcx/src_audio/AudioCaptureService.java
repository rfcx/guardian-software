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

	private RfcxSource rfcxSource = null;
	
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
		if (RfcxSource.VERBOSE) Log.d(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		rfcxSource = (RfcxSource) getApplication();
		rfcxSource.isServiceRunning_AudioCapture = true;
		this.audioCapture.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		rfcxSource.isServiceRunning_AudioCapture = false;
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
			rfcxSource = (RfcxSource) getApplicationContext();
			AudioState audioState = rfcxSource.audioState;
			try {
				int bufferSize = 8 * AudioRecord.getMinBufferSize(
						AudioState.CAPTURE_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
						AudioFormat.ENCODING_PCM_16BIT);
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, AudioState.CAPTURE_SAMPLE_RATE,
						AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,
						bufferSize);
				short[] audioBuffer = new short[AudioState.BUFFER_LENGTH];
				audioRecord.startRecording();
				
				while (audioCaptureService.runFlag) {
					try {
						audioRecord.read(audioBuffer, 0, AudioState.BUFFER_LENGTH);
						audioState.addSpectrum(audioBuffer, rfcxSource);
					} catch (Exception e) {
						audioCaptureService.runFlag = false;
						rfcxSource.isServiceRunning_AudioCapture = false;
					}
				}
				if (RfcxSource.VERBOSE) Log.d(TAG, "Stopping service: "+TAG);
				audioRecord.stop();
			} catch (Exception e) {
				audioCaptureService.runFlag = false;
				rfcxSource.isServiceRunning_AudioCapture = false;
			}
		}
	}

}
