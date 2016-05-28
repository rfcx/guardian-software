package org.rfcx.guardian.encode.service;

import org.rfcx.guardian.encode.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class AudioEncodeTrigger extends Service {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioEncodeTrigger.class.getSimpleName();
	
	private static final String SERVICE_NAME = "AudioEncodeTrigger";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AudioEncode audioEncodeTrigger;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.audioEncodeTrigger = new AudioEncode();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.audioEncodeTrigger.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(TAG, e);
		}
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.audioEncodeTrigger.interrupt();
		this.audioEncodeTrigger = null;
	}
	
	
	private class AudioEncode extends Thread {
		
		public AudioEncode() {
			super("AudioEncodeTrigger-AudioEncode");
		}
		
		@Override
		public void run() {
			AudioEncodeTrigger audioEncodeTrigger = AudioEncodeTrigger.this;
			
			app = (RfcxGuardian) getApplication();

			long audioEncodeTriggerCyclePause = (long) (3 * app.rfcxPrefs.getPrefAsInt("audio_encode_cycle_pause"));
			long audioEncodeLoopTimeOut = (long) (3 * app.rfcxPrefs.getPrefAsInt("audio_cycle_duration"));
			
			try {
				Log.d(TAG, "AudioEncodeTrigger Period: "+ audioEncodeTriggerCyclePause +"ms");
				while (audioEncodeTrigger.runFlag) {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
					try {
				        Thread.sleep(audioEncodeTriggerCyclePause);
				        app.rfcxServiceHandler.triggerOrForceReTriggerIfTimedOut("AudioEncode", audioEncodeLoopTimeOut);

					} catch (Exception e) {
						RfcxLog.logExc(TAG, e);
						app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
						audioEncodeTrigger.runFlag = false;
					}
				}
				Log.v(TAG, "Stopping service: "+TAG);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				audioEncodeTrigger.runFlag = false;
				
			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				audioEncodeTrigger.runFlag = false;
			}
		}
	}

	
}
