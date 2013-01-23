package org.rfcx.src_audio;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioCaptureService extends Service {

	private static final String TAG = AudioCaptureService.class.getSimpleName();

	AudioState audioState = new AudioState();

	private boolean runFlag = false;
	private AudioCapture audioCapture;
	
	private int audioCaptureSampleRate = audioState.audioCaptureSampleRate;
	private int audioCaptureFrameSize = audioState.fftBlockSize;
	private int audioCaptureChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private int audioCaptureEncoding = AudioFormat.ENCODING_PCM_16BIT;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
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

			try {
				int bufferSize = 4 * AudioRecord.getMinBufferSize(
						audioCaptureSampleRate, audioCaptureChannelConfig,
						audioCaptureEncoding);
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, audioCaptureSampleRate,
						audioCaptureChannelConfig, audioCaptureEncoding,
						bufferSize);
				short[] buffer = new short[audioCaptureFrameSize];
				double[] audioFrame = new double[audioCaptureFrameSize];
				audioRecord.startRecording();
				Log.d(TAG, "AudioCaptureService started (buffer: "+bufferSize+")");
				
				while (audioCaptureService.runFlag) {
					try {
						int bufferReadResult = audioRecord.read(buffer, 0, audioCaptureFrameSize);
						for (int i = 0; i < audioCaptureFrameSize && i < bufferReadResult; i++) {
							audioFrame[i] = (double) buffer[i] / 32768.0;
						}
						audioState.addFrame(audioFrame);
					} catch (Exception e) {
						audioCaptureService.runFlag = false;
					}
				}
				audioRecord.stop();
			} catch (Exception e) {
				audioCaptureService.runFlag = false;
			}
		}
	}

}
