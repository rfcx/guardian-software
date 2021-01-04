package org.rfcx.guardian.guardian.audio.playback;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class AudioPlaybackJobService extends Service {

	private static final String SERVICE_NAME = "AudioPlaybackJob";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioPlaybackJobService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioPlaybackJob audioPlaybackJob;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioPlaybackJob = new AudioPlaybackJob();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioPlaybackJob.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.audioPlaybackJob.interrupt();
		this.audioPlaybackJob = null;
	}
	
	private class AudioPlaybackJob extends Thread {

		public AudioPlaybackJob() {
			super("AudioPlaybackJobService-AudioPlaybackJob");
		}
		
		@Override
		public void run() {
			AudioPlaybackJobService audioPlaybackJobInstance = AudioPlaybackJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			
			try {

				Log.i(logTag, "Audio Playback Job...");
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				audioPlaybackJobInstance.runFlag = false;
			}
			
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			audioPlaybackJobInstance.runFlag = false;

		}
	}
	

}
