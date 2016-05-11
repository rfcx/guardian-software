package org.rfcx.guardian.system.service;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.system.device.DeviceScreenShot;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class DeviceScreenShotService extends Service {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceScreenShotService.class.getSimpleName();
	
	RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceScreenShotSvc deviceScreenShotSvc;
	
	private DeviceScreenShot deviceScreenShot = new DeviceScreenShot();
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceScreenShotSvc = new DeviceScreenShotSvc();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(TAG, "Starting service: "+TAG);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState("ScreenShot", true);
		try {
			this.deviceScreenShotSvc.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(TAG, e);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState("ScreenShot", false);
		this.deviceScreenShotSvc.interrupt();
		this.deviceScreenShotSvc = null;
	}
	
	private class DeviceScreenShotSvc extends Thread {

		public DeviceScreenShotSvc() {
			super("DeviceScreenShotService-DeviceScreenShotSvc");
		}

		@Override
		public void run() {
			DeviceScreenShotService deviceScreenShotService = DeviceScreenShotService.this;

			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			try {
				// activate screen and set wake lock
				app.deviceScreenLock.unLockScreen(context);
				Thread.sleep(3000);
				
				String[] saveScreenShot = deviceScreenShot.launchCapture(context);
				if (saveScreenShot != null) { 
					app.screenShotDb.dbCaptured.insert(saveScreenShot[0], saveScreenShot[1], saveScreenShot[2], saveScreenShot[3]);
					Log.i(TAG, "ScreenShot saved: "+saveScreenShot[0]+"."+saveScreenShot[1]);
				}
				Thread.sleep(3000);

			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
			} finally {
				deviceScreenShotService.runFlag = false;
				app.rfcxServiceHandler.setRunState("ScreenShot", false);
				app.rfcxServiceHandler.stopService("ScreenShot");
				app.deviceScreenLock.releaseWakeLock();
			}
		}
	}

}
