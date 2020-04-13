package org.rfcx.guardian.updater.service;

import java.io.File;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class InstallAppService extends Service {

	private static final String SERVICE_NAME = "InstallApp";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstallAppService");

	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private InstallApp installApp;
	
	private int installLoopCounter = 0;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.installApp = new InstallApp();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.installApp.start();
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
		this.installApp.interrupt();
		this.installApp = null;
	}
	
	private class InstallApp extends Thread {

		public InstallApp() {
			super("InstallAppService-InstallApp");
		}

		@Override
		public void run() {
			InstallAppService installAppService = InstallAppService.this;
			boolean successfullyInstalled = false;
			String apkFilePath = app.installUtils.apkDirExternal+"/"+app.apiCheckVersionUtils.installRole+"-"+app.apiCheckVersionUtils.installVersion+".apk";
			try {
//				ShellCommands.killProcessByName("org.rfcx.guardian."+app.targetAppRole,"."+RfcxGuardian.APP_ROLE, app.getApplicationContext());
				successfullyInstalled = installApk(app.targetAppRole, app.getApplicationContext(),apkFilePath);
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				
			//	String apkFilePath = app.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+apkFileName;
				File apkFile = (new File(apkFilePath));
				
				if (successfullyInstalled) {
					Log.d(logTag, "installation successful ("+app.targetAppRole+", "+app.apiCheckVersionUtils.installVersion+"). deleting apk and rebooting...");
					installLoopCounter = 0;
//					if (apkFile.exists()) apkFile.delete();
//				DeviceReboot.triggerForcedRebootAsRoot(app.getApplicationContext());
				} else if (	(installLoopCounter < 1) && FileUtils.sha1Hash(apkFilePath).equals(app.apiCheckVersionUtils.installVersionSha1)) {
					installLoopCounter++;
					app.rfcxServiceHandler.triggerService(SERVICE_NAME, true);
				} else {
					Log.d(logTag, "installation failed ("+app.targetAppRole+", "+app.apiCheckVersionUtils.installVersion+").  deleting apk...");
					installLoopCounter = 0;
//					if (apkFile.exists()) apkFile.delete();
				}

				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}
	
	private boolean installApk(String targetAppRole, Context context, String apkFilePath) {

		File apkFileObj = new File(apkFilePath);
		Log.d(logTag, "Installing APK: "+apkFilePath);
		try {

			String[] installCommands = new String[] { "/system/bin/pm", "install", "-r", apkFilePath };
			if (app.apiCheckVersionUtils.installVersion == null) installCommands = new String[] { "/system/bin/pm", "install", apkFilePath };

			Process shellProcess = Runtime.getRuntime().exec(installCommands);
			shellProcess.waitFor();

			String targetAppVersion = RfcxRole.getRoleVersionByName(targetAppRole, RfcxGuardian.APP_ROLE, context);
			Log.e(logTag, "Installed App Version: "+targetAppVersion);

			if (apkFileObj.exists()) { apkFileObj.delete(); }
			return (targetAppVersion != null);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			if (apkFileObj.exists()) { apkFileObj.delete(); }
		} finally {
		}
		return false;
	}

}
