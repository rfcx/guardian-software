package org.rfcx.rfcx_src_android;

import java.text.DecimalFormat;

import ca.uol.aig.fftpack.RealDoubleFFT;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioCaptureService extends Service {

	static final String TAG = "AudioCaptureService";

    private int audioCaptureSampleRate = 44100;
    private int fftBlockSize = 2048;
    private int fftSigFig = 4;
    
	private boolean runFlag = false;
	private AudioCapture audioCapture;
	AudioCaptureDbHelper audioCaptureDbHelper = new AudioCaptureDbHelper(this);
    private int audioCaptureChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioCaptureEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private RealDoubleFFT transformer;
    private DecimalFormat decimalFormat = new DecimalFormat("#");
    private double fftSigFigMultiplier = Math.pow(10, fftSigFig);
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.audioCapture = new AudioCapture();
		audioCaptureDbHelper = new AudioCaptureDbHelper(this);
		Log.d(TAG, "onCreated()");
		transformer = new RealDoubleFFT(fftBlockSize);
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
				int bufferSize = AudioRecord.getMinBufferSize(audioCaptureSampleRate,audioCaptureChannelConfig, audioCaptureEncoding);
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, audioCaptureSampleRate,
						audioCaptureChannelConfig, audioCaptureEncoding, bufferSize);
				short[] buffer = new short[fftBlockSize];
				double[] toTransform = new double[fftBlockSize];
				
				audioRecord.startRecording();
				Log.d(TAG, "AudioCaptureService started");
				while (audioCaptureService.runFlag) {
					try {
						int bufferReadResult = audioRecord.read(buffer, 0, fftBlockSize);
						for (int i = 0; i < fftBlockSize && i < bufferReadResult; i++) {
							toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
						}
						transformer.ft(toTransform);
						Log.d(TAG, concatValues(toTransform));
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
	
	
	private String concatValues(double[] values) {
		StringBuilder sbFFT = new StringBuilder();
		sbFFT.append(decimalFormat.format(java.lang.Math.abs(values[0] * fftSigFigMultiplier)));
		for (int i = 1; i < fftBlockSize; i++) {
			sbFFT.append(",").append(decimalFormat.format(java.lang.Math.abs(values[i] * fftSigFigMultiplier)));
		}
		return sbFFT.toString();
	}
	
}
