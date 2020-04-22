package org.rfcx.guardian.admin.device.android.capture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.camera.RfcxCameraUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class CameraVideoCaptureService extends Service {

	private static final String SERVICE_NAME = "CameraVideoCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CameraVideoCaptureService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private CameraVideoCapture cameraVideoCapture;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.cameraVideoCapture = new CameraVideoCapture();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.cameraVideoCapture.start();
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
		this.cameraVideoCapture.interrupt();
		this.cameraVideoCapture = null;
	}

	private class CameraVideoCapture extends Thread {

		public CameraVideoCapture() {
			super("CameraVideoCaptureService-CameraVideoCapture");
		}

		@Override
		public void run() {
			CameraVideoCaptureService cameraVideoCaptureInstance = CameraVideoCaptureService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			RfcxCameraUtils rfcxCameraUtils = new RfcxCameraUtils(context, RfcxGuardian.APP_ROLE, app.rfcxGuardianIdentity.getGuid());
			String videoCaptureDir = RfcxCameraUtils.videoCaptureDir(context);
			FileUtils.deleteDirectoryContents(videoCaptureDir);

			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				if (	 confirmOrSetCameraVideoCaptureParameters() ) {

					Log.e(logTag, "CURRENTLY THIS SERVICE DOES ABSOLUTELY NOTHING. IT'S JUST A WRAPPER.");
					Log.e(logTag, "NO VIDEO CAPTURED. PLEASE ADD THE REQUIRED FUNCTIONALITY.");

				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				cameraVideoCaptureInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}
	
	private boolean confirmOrSetCameraVideoCaptureParameters() {
		
		if (app != null) {
			
			
		} else {
			return false;
		}
		
		return true;
	}
	
	
}
