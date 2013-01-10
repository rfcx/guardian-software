package org.rfcx.rfcx_src_android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ArduinoCommService extends Service {
	
	static final String TAG = "ArduinoCommService";
	
	static final int DELAY = 5000;
	private boolean runFlag = false;
	private ArduinoComm arduinoComm;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.arduinoComm = new ArduinoComm();
		Log.d(TAG, "onCreated()");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		this.arduinoComm.start();
		Log.d(TAG, "onStarted()");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		this.arduinoComm.interrupt();
		this.arduinoComm = null;
		Log.d(TAG, "onDestroyed()");
	}
	
	
	private class ArduinoComm extends Thread {
		
		public ArduinoComm() {
			super("ArduinoCommService-ArduinoComm");
		}
		
		@Override
		public void run() {
			ArduinoCommService arduinoCommService = ArduinoCommService.this;
			while (arduinoCommService.runFlag) {
				Log.d(TAG, "ArduinoCommService running");
				try {
					Log.d(TAG, "ArduinoCommService complete");
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					arduinoCommService.runFlag = false;
				}
			}
		}
		
	}

}
