package org.rfcx.guardian.admin.device.android.capture;

import org.rfcx.guardian.utility.asset.RfcxScreenShotFileUtils;
import org.rfcx.guardian.utility.device.control.DeviceScreenLock;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ScreenShotCaptureService extends Service {

	public static final String SERVICE_NAME = "ScreenShotCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScreenShotCaptureService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ScreenShotCapture screenShotCapture;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.screenShotCapture = new ScreenShotCapture();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.screenShotCapture.start();
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
		this.screenShotCapture.interrupt();
		this.screenShotCapture = null;
	}
	
	
	private class ScreenShotCapture extends Thread {
		
		public ScreenShotCapture() {
			super("ScreenShotCaptureService-ScreenShotCapture");
		}
		
		@Override
		public void run() {
			ScreenShotCaptureService screenShotCaptureInstance = ScreenShotCaptureService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			RfcxScreenShotFileUtils rfcxScreenShotFileUtils = new RfcxScreenShotFileUtils(context, RfcxGuardian.APP_ROLE, app.rfcxGuardianIdentity.getGuid());
			DeviceScreenLock deviceScreenLock = new DeviceScreenLock(RfcxGuardian.APP_ROLE);

			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				// activate screen and set wake lock
				deviceScreenLock.unLockScreen(context);
				Thread.sleep(1500);

				String[] saveScreenShot = rfcxScreenShotFileUtils.launchCapture(context);

				if (saveScreenShot != null) {
					app.screenShotDb.dbCaptured.insert(saveScreenShot[0], saveScreenShot[1], saveScreenShot[2], saveScreenShot[3], saveScreenShot[4], saveScreenShot[5]);
					Log.i(logTag, "ScreenShot saved: "+saveScreenShot[5]);
				}
				Thread.sleep(1500);

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				screenShotCaptureInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME, false);
				deviceScreenLock.releaseWakeLock();
			}
		}
	}

	
}
