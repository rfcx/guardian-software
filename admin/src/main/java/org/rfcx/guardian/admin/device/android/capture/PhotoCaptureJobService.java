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

public class PhotoCaptureJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, PhotoCaptureJobService.class);
		
	private static final String SERVICE_NAME = "PhotoCapture";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private PhotoCaptureJobSvc photoCaptureJobSvc;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.photoCaptureJobSvc = new PhotoCaptureJobSvc();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.photoCaptureJobSvc.start();
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
		this.photoCaptureJobSvc.interrupt();
		this.photoCaptureJobSvc = null;
	}

	private class PhotoCaptureJobSvc extends Thread {

		public PhotoCaptureJobSvc() {
			super("PhotoCaptureJobService-PhotoCaptureJobSvc");
		}

		@Override
		public void run() {
			PhotoCaptureJobService photoCaptureJobServiceInstance = PhotoCaptureJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			
			String photoCaptureDir = RfcxCameraUtils.photoCaptureDir(context);
			FileUtils.deleteDirectoryContents(photoCaptureDir);

			Log.e(logTag, "CURRENTLY THIS SERVICE DOES ABSOLUTELY NOTHING. NO PHOTOS CAPTURED.");

			while (photoCaptureJobServiceInstance.runFlag) {
				
				try {
					
					if (	 confirmOrSetCameraPhotoCaptureParameters() ) {
					
						// do stuff
						
					}
					
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					photoCaptureJobServiceInstance.runFlag = false;
				}
			}
			
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			photoCaptureJobServiceInstance.runFlag = false;
			
			Log.v(logTag, "Stopping service: "+logTag);
				
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
