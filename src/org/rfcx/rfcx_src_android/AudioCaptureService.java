package org.rfcx.rfcx_src_android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AudioCaptureService extends Service {

	static final String TAG = "AudioCaptureService";
	
	static final int DELAY = 5000;
	private boolean runFlag = false;
	private AudioCapture audioCapture;
	
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
			while (audioCaptureService.runFlag) {
				Log.d(TAG, "AudioCaptureService running");
				try {
					Log.d(TAG, "AudioCaptureService complete");
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					audioCaptureService.runFlag = false;
				}
			}
		}
		
	}
	
}
