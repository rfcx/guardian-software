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

public class DeviceLogCatQueueService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceLogCatQueueService.class);
	
	private static final String SERVICE_NAME = "DeviceLogCatQueue";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceLogCatQueue deviceLogCatQueue;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceLogCatQueue = new DeviceLogCatQueue();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.deviceLogCatQueue.start();
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
		this.deviceLogCatQueue.interrupt();
		this.deviceLogCatQueue = null;
	}
	
	
	private class DeviceLogCatQueue extends Thread {
		
		public DeviceLogCatQueue() {
			super("DeviceLogCatQueueService-DeviceLogCatQueue");
		}
		
		@Override
		public void run() {
			DeviceLogCatQueueService deviceLogCatQueueInstance = DeviceLogCatQueueService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			try {
				
				List<String[]> latestLogCatCaptureFiles = app.deviceLogCatDb.dbCaptured.getAllRows();
				if (latestLogCatCaptureFiles.size() == 0) { Log.d(logTag, "No log files have been captured."); }
				
				for (String[] latestLogCatCapture : latestLogCatCaptureFiles) {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
					
					if (latestLogCatCapture[0] != null) {
						
						File postCaptureFile = new File(latestLogCatCapture[4]);
						
						if (!postCaptureFile.exists()) {
							
							app.deviceLogCatDb.dbCaptured.deleteSingleRowByTimestamp(latestLogCatCapture[1]);
							
						} else {
							
							File finalGzipFile = new File(DeviceLogCat.getLogFileLocation_Complete_PostZip(app.rfcxDeviceGuid.getDeviceGuid(), context, (long) Long.parseLong(latestLogCatCapture[1])));
							
							if (finalGzipFile.exists()) { finalGzipFile.delete(); }
							
							String preGzipDigest = FileUtils.sha1Hash(postCaptureFile.getAbsolutePath());
							
							FileUtils.gZipFile(postCaptureFile, finalGzipFile);
							
							if (finalGzipFile.exists()) {

								FileUtils.chmod(finalGzipFile, 0777);
								if (postCaptureFile.exists()) { postCaptureFile.delete(); }
								
								app.deviceLogCatDb.dbQueued.insert(latestLogCatCapture[1], DeviceLogCat.FILETYPE, preGzipDigest, finalGzipFile.getAbsolutePath());
							
								app.deviceLogCatDb.dbCaptured.deleteSingleRowByTimestamp(latestLogCatCapture[1]);
								
							}
							
						}
						
					}
					
				}

				// removing older files if they're left in the post-encode directory for more than a day
				FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(DeviceLogCat.postCaptureDir(context), 1440);
			
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				deviceLogCatQueueInstance.runFlag = false;	
			}
	
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			deviceLogCatQueueInstance.runFlag = false;
		
		}
	}

	
}
