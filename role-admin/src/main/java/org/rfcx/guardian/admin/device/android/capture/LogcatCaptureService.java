package org.rfcx.guardian.admin.device.android.capture;

import org.rfcx.guardian.utility.asset.RfcxLogcatFileUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class LogcatCaptureService extends Service {

	public static final String SERVICE_NAME = "LogcatCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "LogcatCaptureService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private LogcatCapture logcatCapture;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.logcatCapture = new LogcatCapture();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.logcatCapture.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxSvc.setRunState(SERVICE_NAME, false);
		this.logcatCapture.interrupt();
		this.logcatCapture = null;
	}
	
	
	private class LogcatCapture extends Thread {
		
		public LogcatCapture() {
			super("LogcatCaptureService-LogcatCapture");
		}
		
		@Override
		public void run() {
			LogcatCaptureService logcatCaptureInstance = LogcatCaptureService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			RfcxLogcatFileUtils rfcxLogcatFileUtils = new RfcxLogcatFileUtils(context, RfcxGuardian.APP_ROLE, app.rfcxGuardianIdentity.getGuid(), app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_LOG_CAPTURE_LEVEL));
			String scriptFilePath = RfcxLogcatFileUtils.getLogcatExecutableScriptFilePath(context);

			// removing older files if they're left in the capture directory
			FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(RfcxLogcatFileUtils.logcatCaptureDir(context), 60);
			
			try {
				
				app.rfcxSvc.reportAsActive(SERVICE_NAME);
				
				long captureCycleBeginningTimeStamp = System.currentTimeMillis();
				
				String captureFilePath = RfcxLogcatFileUtils.getLogcatFileLocation_Capture(context, captureCycleBeginningTimeStamp);
				String postCaptureFilePath = RfcxLogcatFileUtils.getLogcatFileLocation_PostCapture(context, captureCycleBeginningTimeStamp);
				long scriptDurationInSeconds = Math.round((app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.ADMIN_LOG_CAPTURE_CYCLE) * 60) * 0.9);
				
				String execCmd = scriptFilePath+" "+captureFilePath+" "+postCaptureFilePath+" "+scriptDurationInSeconds;
				ShellCommands.executeCommandAsRootAndIgnoreOutput(execCmd);

				long captureCycleEndingTimeStamp = System.currentTimeMillis();

				// removing older files if they're left in the 'postCapture' directory
				FileUtils.deleteDirectoryContentsIfOlderThanExpirationAge(RfcxLogcatFileUtils.logcatPostCaptureDir(context), 60);
				
				File postCaptureFile = new File(postCaptureFilePath);
				
				if (!postCaptureFile.exists()) {
					Log.e(logTag, "could not find captured log file: "+postCaptureFilePath);
				} else {
					
					File finalGzipFile = new File(RfcxLogcatFileUtils.getLogcatFileLocation_Queue(app.rfcxGuardianIdentity.getGuid(), context, captureCycleEndingTimeStamp ));
					
					if (finalGzipFile.exists()) { finalGzipFile.delete(); }
					
					String preGzipDigest = FileUtils.sha1Hash(postCaptureFile.getAbsolutePath());
					
					FileUtils.gZipFile(postCaptureFile, finalGzipFile);
					
					if (finalGzipFile.exists()) {

						FileUtils.chmod(finalGzipFile, "rw", "rw");
						if (postCaptureFile.exists()) { postCaptureFile.delete(); }

						app.logcatDb.dbCaptured.insert(captureCycleEndingTimeStamp+"", RfcxLogcatFileUtils.FILETYPE, preGzipDigest, finalGzipFile.getAbsolutePath());
						
						Log.i(logTag, "Logcat Snapshot Saved: "+finalGzipFile.getAbsolutePath());
					}
					
				}
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxSvc.setRunState(SERVICE_NAME, false);
				logcatCaptureInstance.runFlag = false;
			}
			
			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			logcatCaptureInstance.runFlag = false;
			
		}
	}

	
}
