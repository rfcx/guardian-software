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
	
	private boolean runFlag = false;
	private AudioCapture audioCapture;
	
	AudioCaptureDbHelper audioCaptureDbHelper = new AudioCaptureDbHelper(this);
	
    private int audioCaptureSampleRate = 44100;
    private int audioCaptureChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioCaptureEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private RealDoubleFFT transformer;
    private int fftBlockSize = 2048;
    
    private DecimalFormat decimalFormat = new DecimalFormat("#");
	
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
		
		//borrowed
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
						String console = "";
						for (int i = 0; i < 16; i++) {
							float val = (float) (java.lang.Math.abs(toTransform[i] * 1000));
							console += "\t" + decimalFormat.format(val);
						}
						Log.i(TAG,console);
						
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
