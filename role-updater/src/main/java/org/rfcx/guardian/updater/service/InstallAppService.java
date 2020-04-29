package org.rfcx.guardian.updater.service;

import java.io.File;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.w3c.dom.Text;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
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

			boolean isSuccessfullyInstalled = false;
			String apkFilePath = app.installUtils.apkDirExternal+"/"+app.apiCheckVersionUtils.installRole+"-"+app.apiCheckVersionUtils.installVersion+".apk";
			try {
//				ShellCommands.killProcessByName("org.rfcx.guardian."+app.targetAppRole,"."+RfcxGuardian.APP_ROLE, app.getApplicationContext());
				isSuccessfullyInstalled = installApk(app.targetAppRole, app.apiCheckVersionUtils.installVersion, app.getApplicationContext(), apkFilePath);

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);

			} finally {
				
				if (isSuccessfullyInstalled) {
					Log.d(logTag, "Installation Successful: "+app.targetAppRole+", "+app.apiCheckVersionUtils.installVersion);
					installLoopCounter = 0;
//					Log.d(logTag, "Rebooting system now...");
//					app.rfcxServiceHandler.triggerService("RebootTrigger", true);
//				} else if (	(installLoopCounter < 1) && FileUtils.sha1Hash(apkFilePath).equals(app.apiCheckVersionUtils.installVersionSha1)) {
//					installLoopCounter++;
//					app.rfcxServiceHandler.triggerService(SERVICE_NAME, true);
				} else {
					Log.e(logTag, "Installation Failed: "+app.targetAppRole+", "+app.apiCheckVersionUtils.installVersion);
					installLoopCounter = 0;
					app.apiCheckVersionUtils.lastCheckInTriggered = 0;
				}

				if ((new File(apkFilePath)).exists()) (new File(apkFilePath)).delete();

				app.apiCheckVersionUtils.lastCheckInTriggered = 0;

				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}
	
	private boolean installApk(String targetAppRole, String targetAppNewVersion, Context context, String apkFilePath) {

		Log.d(logTag, "Installing APK: "+apkFilePath);
		try {

			String[] installCommands = new String[] { "/system/bin/pm", "install", "-r", apkFilePath };
			if (app.apiCheckVersionUtils.installVersion == null) installCommands = new String[] { "/system/bin/pm", "install", apkFilePath };

			ShellCommands.executeCommand(TextUtils.join(" ", installCommands));

			String currentAppVersion = RfcxRole.getRoleVersionByName(targetAppRole, RfcxGuardian.APP_ROLE, context);
			Log.e(logTag, "Current App Version Installed: "+currentAppVersion);

			return (currentAppVersion == targetAppNewVersion);

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
		}
		return false;
	}

}
