package admin.device.android.capture;

import rfcx.utility.device.control.DeviceLogCat;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.misc.ShellCommands;
import rfcx.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.List;

import admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DeviceLogCatCaptureService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceLogCatCaptureService.class);
	
	private static final String SERVICE_NAME = "LogCatCapture";
	
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
		return START_STICKY;
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

			String scriptFilePath = DeviceLogCat.getExecutableScriptFilePath(context);

			// removing older files if they're left in the capture directory
			FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(DeviceLogCat.captureDir(context), 60);
			
			try {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				
				long captureCycleBeginningTimeStamp = System.currentTimeMillis();
				
				String captureFilePath = DeviceLogCat.getLogFileLocation_Capture(context, captureCycleBeginningTimeStamp);
				String postCaptureFilePath = DeviceLogCat.getLogFileLocation_PostCapture(context, captureCycleBeginningTimeStamp);
				
				String execCmd = scriptFilePath+" "+captureFilePath+" "+postCaptureFilePath+" "+3;
				ShellCommands.executeCommandAsRootAndIgnoreOutput(execCmd, context);

				long captureCycleEndingTimeStamp = System.currentTimeMillis();

				// removing older files if they're left in the 'postCapture' directory
				FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(DeviceLogCat.postCaptureDir(context), 60);
				
				File postCaptureFile = new File(postCaptureFilePath);
				
				if (!postCaptureFile.exists()) {
					Log.e(logTag, "could not find captured log file: "+postCaptureFilePath);
				} else {
					
					File finalGzipFile = new File(DeviceLogCat.getLogFileLocation_Complete_PostZip(app.rfcxDeviceGuid.getDeviceGuid(), context, captureCycleEndingTimeStamp ));
					
					if (finalGzipFile.exists()) { finalGzipFile.delete(); }
					
					String preGzipDigest = FileUtils.sha1Hash(postCaptureFile.getAbsolutePath());
					
					FileUtils.gZipFile(postCaptureFile, finalGzipFile);
					
					if (finalGzipFile.exists()) {

						FileUtils.chmod(finalGzipFile, 0777);
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
