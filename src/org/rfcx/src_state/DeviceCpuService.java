package org.rfcx.src_state;

import org.rfcx.rfcx_src_android.RfcxSource;
import org.rfcx.src_util.DeviceCpuUsage;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DeviceCpuService extends Service {

	private static final String TAG = DeviceCpuService.class.getSimpleName();
	static final int DELAY = 500;
	private boolean runFlag = false;
	private CpuServiceCheck cpuServiceCheck;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.cpuServiceCheck = new CpuServiceCheck();
		Log.d(TAG, "onCreate()");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		this.cpuServiceCheck.start();
		Log.d(TAG, "onStart()");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		this.cpuServiceCheck.interrupt();
		this.cpuServiceCheck = null;
		Log.d(TAG, "onDestroy()");
	}
	
	private class CpuServiceCheck extends Thread {
		
		public CpuServiceCheck() {
			super("CpuServiceCheck-CpuService");
		}
		
		@Override
		public void run() {
			DeviceCpuService cpuService = DeviceCpuService.this;
			DeviceCpuUsage deviceCpuUsage = ((RfcxSource) getApplication()).deviceCpuUsage;
			while (cpuService.runFlag) {
				try {
					deviceCpuUsage.updateCpuUsage();
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					cpuService.runFlag = false;
				}
			}
		}		
	}

}
