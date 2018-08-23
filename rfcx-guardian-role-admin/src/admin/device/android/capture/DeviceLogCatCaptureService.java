package admin.device.android.capture;

import rfcx.utility.device.control.DeviceLogCat;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.misc.ShellCommands;
import rfcx.utility.rfcx.RfcxLog;

import java.util.List;

import admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DeviceLogCatCaptureService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceLogCatCaptureService.class);
	
	private static final String SERVICE_NAME = "DeviceLogCatCapture";
	
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
			int captureCycleDuration = 0;

			// removing older files if they're left in the capture directory
			FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(DeviceLogCat.captureDir(context), 60);
			
			while (deviceLogCatCaptureInstance.runFlag) {
				
				try {
					
					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
					if (app.rfcxPrefs.getPrefAsInt("admin_log_capture_cycle") != captureCycleDuration) {
						captureCycleDuration = app.rfcxPrefs.getPrefAsInt("admin_log_capture_cycle");
						Log.d(logTag, (new StringBuilder()).append("LogCat Capture Params: ").append(captureCycleDuration).append(" seconds").toString());
					}
					
					long captureCycleBeginningTimeStamp = System.currentTimeMillis();
					
					String captureFilePath = DeviceLogCat.getLogFileLocation_Capture(context, captureCycleBeginningTimeStamp);
					String postCaptureFilePath = DeviceLogCat.getLogFileLocation_PostCapture(context, captureCycleBeginningTimeStamp);
					
					String execCmd = scriptFilePath+" "+captureFilePath+" "+postCaptureFilePath+" "+captureCycleDuration;
					ShellCommands.executeCommandAsRootAndIgnoreOutput(/*"nohup "+*/execCmd+"&", context);
					
					app.deviceLogCatDb.dbCaptured.insert(captureCycleBeginningTimeStamp+"", DeviceLogCat.FILETYPE, null, postCaptureFilePath);
					app.rfcxServiceHandler.triggerService("DeviceLogCatQueue", true);
					
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					deviceLogCatCaptureInstance.runFlag = false;	
				}
				
			}
			
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			deviceLogCatCaptureInstance.runFlag = false;
			
			Log.v(logTag, "Stopping service: "+logTag);
			
		}
	}

	
}
