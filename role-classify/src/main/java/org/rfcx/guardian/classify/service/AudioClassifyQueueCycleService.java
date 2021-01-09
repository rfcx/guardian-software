package org.rfcx.guardian.classify.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class AudioClassifyQueueCycleService extends Service {

	public static final String SERVICE_NAME = "AudioClassifyQueueCycle";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyQueueCycleService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioClassifyQueueCycleSvc audioClassifyQueueCycleSvc;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioClassifyQueueCycleSvc = new AudioClassifyQueueCycleSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioClassifyQueueCycleSvc.start();
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
		this.audioClassifyQueueCycleSvc.interrupt();
		this.audioClassifyQueueCycleSvc = null;
	}
	
	
	private class AudioClassifyQueueCycleSvc extends Thread {
		
		public AudioClassifyQueueCycleSvc() { super("AudioClassifyQueueCycleService-AudioClassifyQueueCycleSvc"); }
		
		@Override
		public void run() {
			AudioClassifyQueueCycleService audioClassifyQueueCycleInstance = AudioClassifyQueueCycleService.this;
			
			app = (RfcxGuardian) getApplication();

			while (audioClassifyQueueCycleInstance.runFlag) {

				try {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

					if (app.audioClassifyDb.dbQueued.getCount() > 0) {

						app.rfcxServiceHandler.triggerService("AudioClassifyJob", false);

					}

//					if (app.audioClassifyDb.dbQueued.getCount() > 0) {
//
//						app.rfcxServiceHandler.triggerService("AudioClassifyJob", false);
//
//					}

					Thread.sleep( Math.round( app.rfcxPrefs.getPrefAsFloat(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) / 4 ) );

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					audioClassifyQueueCycleInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			audioClassifyQueueCycleInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
