package org.rfcx.guardian.admin.device.android.capture;

import org.rfcx.guardian.utility.device.capture.DeviceLogCat;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DeviceLogCatCaptureService extends Service {

	private static final String SERVICE_NAME = "LogCatCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DeviceLogCatCaptureService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceLogCatCapture deviceLogCatCapture;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceLogCatCapture = new DeviceLogCatCapture();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceLogCatCapture.start();
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
		this.deviceLogCatCapture.interrupt();
		this.deviceLogCatCapture = null;
	}
	
	
	private class DeviceLogCatCapture extends Thread {
		
		public DeviceLogCatCapture() {
			super("DeviceLogCatCaptureService-DeviceLogCatCapture");
		}
		
		@Override
		public void run() {
			DeviceLogCatCaptureService deviceLogCatCaptureInstance = DeviceLogCatCaptureService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			DeviceLogCat deviceLogCat = new DeviceLogCat(context, RfcxGuardian.APP_ROLE, app.rfcxGuardianIdentity.getGuid(), app.rfcxPrefs.getPrefAsString("admin_log_capture_level"));
			String scriptFilePath = DeviceLogCat.getLogExecutableScriptFilePath(context);

			// removing older files if they're left in the capture directory
			FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(DeviceLogCat.logCaptureDir(context), 60);
			
			try {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				
				long captureCycleBeginningTimeStamp = System.currentTimeMillis();
				
				String captureFilePath = DeviceLogCat.getLogFileLocation_Capture(context, captureCycleBeginningTimeStamp);
				String postCaptureFilePath = DeviceLogCat.getLogFileLocation_PostCapture(context, captureCycleBeginningTimeStamp);
				long scriptDurationInSeconds = Math.round((app.rfcxPrefs.getPrefAsLong("admin_log_capture_cycle") * 60) * 0.9);
				
				String execCmd = scriptFilePath+" "+captureFilePath+" "+postCaptureFilePath+" "+scriptDurationInSeconds;
				ShellCommands.executeCommandAsRootAndIgnoreOutput(execCmd, context);

				long captureCycleEndingTimeStamp = System.currentTimeMillis();

				// removing older files if they're left in the 'postCapture' directory
				FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(DeviceLogCat.logPostCaptureDir(context), 60);
				
				File postCaptureFile = new File(postCaptureFilePath);
				
				if (!postCaptureFile.exists()) {
					Log.e(logTag, "could not find captured log file: "+postCaptureFilePath);
				} else {
					
					File finalGzipFile = new File(DeviceLogCat.getLogFileLocation_Complete_PostZip(app.rfcxGuardianIdentity.getGuid(), context, captureCycleEndingTimeStamp ));
					
					if (finalGzipFile.exists()) { finalGzipFile.delete(); }
					
					String preGzipDigest = FileUtils.sha1Hash(postCaptureFile.getAbsolutePath());
					
					FileUtils.gZipFile(postCaptureFile, finalGzipFile);
					
					if (finalGzipFile.exists()) {

						FileUtils.chmod(finalGzipFile, "rw", "rw");
						if (postCaptureFile.exists()) { postCaptureFile.delete(); }

						app.deviceLogCatDb.dbCaptured.insert(captureCycleEndingTimeStamp+"", DeviceLogCat.FILETYPE, preGzipDigest, finalGzipFile.getAbsolutePath());
						
						Log.i(logTag, "LogCat snapshot saved: "+finalGzipFile.getAbsolutePath());
					}
					
				}
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				deviceLogCatCaptureInstance.runFlag = false;	
			}
			
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			deviceLogCatCaptureInstance.runFlag = false;
			
		}
	}

	
}
