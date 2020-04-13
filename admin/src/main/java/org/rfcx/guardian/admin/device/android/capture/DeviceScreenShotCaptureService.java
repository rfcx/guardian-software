package org.rfcx.guardian.admin.device.android.capture;

import org.rfcx.guardian.utility.device.control.DeviceScreenLock;
import org.rfcx.guardian.utility.device.capture.DeviceScreenShot;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DeviceScreenShotCaptureService extends Service {

	private static final String SERVICE_NAME = "ScreenShotCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DeviceScreenShotCaptureService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceScreenShotCapture deviceScreenShotCapture;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceScreenShotCapture = new DeviceScreenShotCapture();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceScreenShotCapture.start();
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
		this.deviceScreenShotCapture.interrupt();
		this.deviceScreenShotCapture = null;
	}
	
	
	private class DeviceScreenShotCapture extends Thread {
		
		public DeviceScreenShotCapture() {
			super("DeviceScreenShotCaptureService-DeviceScreenShotCapture");
		}
		
		@Override
		public void run() {
			DeviceScreenShotCaptureService deviceScreenShotCaptureInstance = DeviceScreenShotCaptureService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			DeviceScreenShot deviceScreenShot = new DeviceScreenShot(context, RfcxGuardian.APP_ROLE, app.rfcxGuardianIdentity.getGuid());
			DeviceScreenLock deviceScreenLock = new DeviceScreenLock(RfcxGuardian.APP_ROLE);
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				// activate screen and set wake lock
				deviceScreenLock.unLockScreen(context);
				Thread.sleep(1500);
				
				String[] saveScreenShot = deviceScreenShot.launchCapture(context);
				if (saveScreenShot != null) { 
					app.deviceScreenShotDb.dbCaptured.insert(saveScreenShot[0], saveScreenShot[1], saveScreenShot[2], saveScreenShot[3]);
					Log.i(logTag, "ScreenShot saved: "+saveScreenShot[3]);
				}
				Thread.sleep(1500);
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				deviceScreenShotCaptureInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
				deviceScreenLock.releaseWakeLock();
			}
		}
	}

	
}
