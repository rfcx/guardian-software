package org.rfcx.guardian.device.system.assets;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.device.DeviceScreenShotUtils;
import org.rfcx.guardian.utility.device.control.DeviceScreenLock;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DeviceScreenShotJobService extends Service {

	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceScreenShotJobService.class.getSimpleName();
	
	private static final String SERVICE_NAME = "DeviceScreenShotJob";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceScreenShotJob deviceScreenShotJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceScreenShotJob = new DeviceScreenShotJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceScreenShotJob.start();
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
		this.deviceScreenShotJob.interrupt();
		this.deviceScreenShotJob = null;
	}
	
	
	private class DeviceScreenShotJob extends Thread {
		
		public DeviceScreenShotJob() {
			super("DeviceScreenShotJobService-DeviceScreenShotJob");
		}
		
		@Override
		public void run() {
			DeviceScreenShotJobService deviceScreenShotJobInstance = DeviceScreenShotJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			DeviceScreenLock deviceScreenLock = new DeviceScreenLock(RfcxGuardian.APP_ROLE);
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				// activate screen and set wake lock
				deviceScreenLock.unLockScreen(context);
				Thread.sleep(3000);
				
				String[] saveScreenShot = DeviceScreenShotUtils.launchCapture(context);
				if (saveScreenShot != null) { 
					app.deviceScreenShotDb.dbCaptured.insert(saveScreenShot[0], saveScreenShot[1], saveScreenShot[2], saveScreenShot[3]);
					Log.i(logTag, "ScreenShot saved: "+saveScreenShot[0]+"."+saveScreenShot[1]);
				}
				Thread.sleep(3000);
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				deviceScreenShotJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
				deviceScreenLock.releaseWakeLock();
			}
		}
	}

	
}
