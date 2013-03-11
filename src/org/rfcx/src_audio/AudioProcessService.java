package org.rfcx.src_audio;

import org.rfcx.src_android.RfcxSource;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AudioProcessService extends Service {
	
	private static final String TAG = AudioProcessService.class.getSimpleName();

	private boolean runFlag = false;
	private AudioProcess audioProcess;
	
	private static final int DELAY = 1000;
	
	private RfcxSource rfcxSource = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioProcess = new AudioProcess();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (RfcxSource.VERBOSE) Log.d(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		rfcxSource = (RfcxSource) getApplication();
		rfcxSource.isServiceRunning_AudioProcess = true;
		this.audioProcess.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		rfcxSource.isServiceRunning_AudioProcess = false;
		this.audioProcess.interrupt();
		this.audioProcess = null;
	}	

	private class AudioProcess extends Thread {

		public AudioProcess() {
			super("AudioProcessService-AudioProcess");
		}
	
		@Override
		public void run() {
			AudioProcessService audioProcessService = AudioProcessService.this;
			rfcxSource = (RfcxSource) getApplicationContext();
			AudioState audioState = rfcxSource.audioState;
			try {
				while (audioProcessService.runFlag) {
					while (audioState.pcmDataBufferLength() > 2) {
						audioState.addSpectrum();
					}
					Thread.sleep(DELAY);
				}
				if (RfcxSource.VERBOSE) Log.d(TAG, "Stopping service: "+TAG);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				audioProcessService.runFlag = false;
				rfcxSource.isServiceRunning_AudioProcess = false;
			}
		}
		
	}
	
}
