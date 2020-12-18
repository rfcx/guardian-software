package org.rfcx.guardian.admin.device.android.capture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxPhotoUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class CameraPhotoCaptureService extends Service {

	private static final String SERVICE_NAME = "CameraPhotoCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CameraPhotoCaptureService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private CameraPhotoCapture cameraPhotoCapture;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.cameraPhotoCapture = new CameraPhotoCapture();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.cameraPhotoCapture.start();
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
		this.cameraPhotoCapture.interrupt();
		this.cameraPhotoCapture = null;
	}

	private class CameraPhotoCapture extends Thread {

		public CameraPhotoCapture() {
			super("CameraPhotoCaptureService-CameraPhotoCapture");
		}

		@Override
		public void run() {
			CameraPhotoCaptureService cameraPhotoCaptureInstance = CameraPhotoCaptureService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			RfcxPhotoUtils rfcxCameraUtils = new RfcxPhotoUtils(context, RfcxGuardian.APP_ROLE, app.rfcxGuardianIdentity.getGuid());
			String photoCaptureDir = RfcxPhotoUtils.photoCaptureDir(context);

			// removing older files if they're left in the capture directory
			FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(photoCaptureDir, 60);

			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				if (	 confirmOrSetCameraPhotoCaptureParameters() ) {

					Log.e(logTag, "CURRENTLY THIS SERVICE DOES ABSOLUTELY NOTHING. IT'S JUST A WRAPPER.");
					Log.e(logTag, "NO PHOTO CAPTURED. PLEASE ADD THE REQUIRED FUNCTIONALITY.");

				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				cameraPhotoCaptureInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}
	
	private boolean confirmOrSetCameraPhotoCaptureParameters() {
		
		if (app != null) {
			
			
		} else {
			return false;
		}
		
		return true;
	}
	
	
}
