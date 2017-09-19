package org.rfcx.guardian.audio.encode;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AudioEncodeLoopService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioEncodeLoopService.class);
	
	private static final String SERVICE_NAME = "AudioEncodeLoop";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioEncodeLoop audioEncodeLoop;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioEncodeLoop = new AudioEncodeLoop();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioEncodeLoop.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.audioEncodeLoop.interrupt();
		this.audioEncodeLoop = null;
	}
	
	
	private class AudioEncodeLoop extends Thread {
		
		public AudioEncodeLoop() {
			super("AudioEncodeLoopService-AudioEncodeLoop");
		}
		
		@Override
		public void run() {
			AudioEncodeLoopService audioEncodeLoopInstance = AudioEncodeLoopService.this;
			
			app = (RfcxGuardian) getApplication();

			long audioEncodeLoopCyclePause = (long) (3 * app.rfcxPrefs.getPrefAsInt("audio_encode_cycle_pause"));
			long audioEncodeLoopTimeOut = (long) (3 * app.rfcxPrefs.getPrefAsInt("audio_cycle_duration"));
			
			try {
				Log.d(logTag, "AudioEncodeLoop Period: "+ audioEncodeLoopCyclePause +"ms");
				while (audioEncodeLoopInstance.runFlag) {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
					try {
				        Thread.sleep(audioEncodeLoopCyclePause);
				        app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("AudioEncodeJob", audioEncodeLoopTimeOut);

					} catch (Exception e) {
						RfcxLog.logExc(logTag, e);
						app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
						audioEncodeLoopInstance.runFlag = false;
					}
				}
				Log.v(logTag, "Stopping service: "+logTag);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				audioEncodeLoopInstance.runFlag = false;
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				audioEncodeLoopInstance.runFlag = false;
			}
		}
	}

	
}
