package org.rfcx.guardian.setup.service;

import java.io.File;

import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import org.rfcx.guardian.setup.RfcxGuardian;

public class ApkInstallService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApkInstallService.class);
	
	private static final String SERVICE_NAME = "ApkInstall";

	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApkInstall apkInstall;
	
	private int installLoopCounter = 0;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apkInstall = new ApkInstall();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.apkInstall.start();
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
		this.apkInstall.interrupt();
		this.apkInstall = null;
	}
	
	private class ApkInstall extends Thread {

		public ApkInstall() {
			super("ApkInstallService-ApkInstall");
		}

		@Override
		public void run() {
			ApkInstallService apkInstallService = ApkInstallService.this;
			boolean successfullyInstalled = false;
			String apkFileName = app.apiCore.installRole+"-"+app.apiCore.installVersion+".apk";
			try {
				ShellCommands.killProcessByName("org.rfcx.org.rfcx.guardian.guardian."+app.targetAppRole,"."+RfcxGuardian.APP_ROLE, app.getApplicationContext());
		//		successfullyInstalled = DeviceAndroidApps.installAndroidApp(apkFilePath, isPreviouslyInstalled, app.getApplicationContext());
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				
				String apkFilePath = app.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+apkFileName;
				File apkFile = (new File(apkFilePath));
				
				if (successfullyInstalled) {
					Log.d(logTag, "installation successful ("+app.targetAppRole+", "+app.apiCore.installVersion+"). deleting apk and rebooting...");
					installLoopCounter = 0;
					if (apkFile.exists()) apkFile.delete();
					ShellCommands.triggerRebootAsRoot(app.getApplicationContext());
				} else if (	(installLoopCounter < 1) && FileUtils.sha1Hash(apkFilePath).equals(app.apiCore.installVersionSha1)) {
					installLoopCounter++;
					app.rfcxServiceHandler.triggerService(SERVICE_NAME, true);
				} else {
					Log.d(logTag, "installation failed ("+app.targetAppRole+", "+app.apiCore.installVersion+").  deleting apk...");
					installLoopCounter = 0;
					if (apkFile.exists()) apkFile.delete();
				}

				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}
	


}
