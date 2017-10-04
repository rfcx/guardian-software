package admin.service;

import org.rfcx.guardian.utility.device.control.DeviceScreenLock;
import org.rfcx.guardian.utility.device.control.DeviceScreenShot;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ScreenShotJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ScreenShotJobService.class);
	
	private static final String SERVICE_NAME = "ScreenShotJob";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ScreenShotJob screenShotJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.screenShotJob = new ScreenShotJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.screenShotJob.start();
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
		this.screenShotJob.interrupt();
		this.screenShotJob = null;
	}
	
	
	private class ScreenShotJob extends Thread {
		
		public ScreenShotJob() {
			super("ScreenShotJobService-ScreenShotJob");
		}
		
		@Override
		public void run() {
			ScreenShotJobService screenShotJobInstance = ScreenShotJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			DeviceScreenShot deviceScreenShot = new DeviceScreenShot(context, RfcxGuardian.APP_ROLE);
			DeviceScreenLock deviceScreenLock = new DeviceScreenLock(RfcxGuardian.APP_ROLE);
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				// activate screen and set wake lock
				deviceScreenLock.unLockScreen(context);
				Thread.sleep(3000);
				
				String[] saveScreenShot = deviceScreenShot.launchCapture(context);
				if (saveScreenShot != null) { 
//					app.deviceScreenShotDb.dbCaptured.insert(saveScreenShot[0], saveScreenShot[1], saveScreenShot[2], saveScreenShot[3]);
					Log.i(logTag, "ScreenShot saved: "+saveScreenShot[0]+"."+saveScreenShot[1]);
				}
				Thread.sleep(3000);
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				screenShotJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
				deviceScreenLock.releaseWakeLock();
			}
		}
	}

	
}
