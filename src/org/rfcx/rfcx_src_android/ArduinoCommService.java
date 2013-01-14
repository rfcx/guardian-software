package org.rfcx.rfcx_src_android;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class ArduinoCommService extends Service {
	
	static final String TAG = "ArduinoCommService";
	
	static final int DELAY = 40000;
	static final int DELAY_INNER = 2000;
	String[] arduinoCommands = new String[] {"a","b"};
	
	private boolean runFlag = false;
	private ArduinoCommSvc arduinoCommSvc;
	
	ArduinoDbHelper arduinoDbHelper = new ArduinoDbHelper(this);
		
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		((RfcxSrcApplication) getApplication()).appResume();
		this.arduinoCommSvc = new ArduinoCommSvc();
		arduinoDbHelper = new ArduinoDbHelper(this);
		Log.d(TAG, "onCreated()");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		this.arduinoCommSvc.start();
		Log.d(TAG, "onStarted()");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		this.arduinoCommSvc.interrupt();
		this.arduinoCommSvc = null;
		Log.d(TAG, "onDestroyed()");
	}
	
	
	private class ArduinoCommSvc extends Thread {
		
		public ArduinoCommSvc() {
			super("ArduinoCommService-ArduinoComm");
		}
		
		@Override
		public void run() {
			ArduinoCommService arduinoCommService = ArduinoCommService.this;
			while (arduinoCommService.runFlag) {
				Log.d(TAG, "ArduinoCommService running");
				try {
					for (int i = 0; i < arduinoCommands.length; i++) {
						((RfcxSrcApplication) getApplication()).sendBtCommand(arduinoCommands[i]);
						Thread.sleep(DELAY_INNER);
					}
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					arduinoCommService.runFlag = false;
				}
			}
		}		
	}
	




}
