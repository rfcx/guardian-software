package org.rfcx.guardian.admin.device.android.capture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxPhotoFileUtils;
import org.rfcx.guardian.utility.asset.RfcxVideoFileUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class CameraCaptureService extends Service {

	public static final String SERVICE_NAME = "CameraCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CameraCaptureService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private CameraCapture cameraCapture;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.cameraCapture = new CameraCapture();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.cameraCapture.start();
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
		this.cameraCapture.interrupt();
		this.cameraCapture = null;
	}

	private class CameraCapture extends Thread {

		public CameraCapture() {
			super("CameraCaptureService-CameraCapture");
		}

		@Override
		public void run() {
			CameraCaptureService cameraCaptureInstance = CameraCaptureService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			RfcxPhotoFileUtils rfcxPhotoUtils = new RfcxPhotoFileUtils(context, RfcxGuardian.APP_ROLE, app.rfcxGuardianIdentity.getGuid());
			String photoCaptureDir = RfcxPhotoFileUtils.photoCaptureDir(context);

			RfcxVideoFileUtils rfcxVideoUtils = new RfcxVideoFileUtils(context, RfcxGuardian.APP_ROLE, app.rfcxGuardianIdentity.getGuid());
			String videoCaptureDir = RfcxVideoFileUtils.videoCaptureDir(context);

			// removing older files if they're left in the capture directory
			FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(photoCaptureDir, 60);
			FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(videoCaptureDir, 60);

			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				if (	 confirmOrSetCameraCaptureParameters() ) {

					Log.e(logTag, "CURRENTLY THIS SERVICE DOES ABSOLUTELY NOTHING. IT'S JUST A WRAPPER.");
					Log.e(logTag, "NO PHOTO OR VIDEO CAPTURED. PLEASE ADD THE REQUIRED FUNCTIONALITY.");

				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				cameraCaptureInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME, false);
			}
		}
	}
	
	private boolean confirmOrSetCameraCaptureParameters() {
		
		if (app != null) {
			
			
		} else {
			return false;
		}
		
		return true;
	}
	
	
}
