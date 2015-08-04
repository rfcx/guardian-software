package org.rfcx.guardian.installer.service;

import java.io.File;

import org.rfcx.guardian.installer.RfcxGuardianInstaller;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class InstallAppService extends Service {

	private static final String TAG = "RfcxGuardianInstaller-"+InstallAppService.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private InstallApp installApp;

	private RfcxGuardianInstaller app = null;
	private Context context = null;
	
	private int installLoopCounter = 0;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.installApp = new InstallApp();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		app = (RfcxGuardianInstaller) getApplication();
		if (context == null) context = app.getApplicationContext();
		
		if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
		
		app.isRunning_InstallApp = true;
		try {
			this.installApp.start();
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		app.isRunning_InstallApp = false;
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
			ShellCommands shellCommands = new ShellCommands();
			boolean successfullyInstalled = false;
			try {
				shellCommands.killProcessByName(context,"org.rfcx.guardian.updater");
				successfullyInstalled = installApk(context,false);
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			} finally {
				
				String apkFilePath = context.getFilesDir().getAbsolutePath()+"/"+app.apiCore.installVersion+".apk";
				File apkFile = (new File(apkFilePath));
				String apkSha1Hash = (new FileUtils()).sha1Hash(apkFilePath);
				
				if (successfullyInstalled) {
					Log.d(TAG, "Installation successful ("+app.apiCore.installVersion+"). Deleting APK and rebooting...");
					installLoopCounter = 0;
					if (apkFile.exists()) apkFile.delete();
					shellCommands.executeCommandAsRoot("reboot",null,context);
				} else if ((installLoopCounter < 1) && apkSha1Hash.equals(app.apiCore.installVersionSha1)) {
					installLoopCounter++;
					app.triggerService("InstallApp", true);
				} else {
					Log.d(TAG, "Installation failed ("+app.apiCore.installVersion+").  Deleting APK...");
					installLoopCounter = 0;
					if (apkFile.exists()) apkFile.delete();
				}

				app.isRunning_DownloadFile = false;
				app.stopService("InstallApp");
			}
		}
	}
	
	private boolean installApk(Context context, boolean forceReInstallFlag) {
		RfcxGuardianInstaller app = (RfcxGuardianInstaller) context.getApplicationContext();
		ShellCommands shellCommands = new ShellCommands();
		File apkFile = new File(context.getFilesDir().getPath(), app.apiCore.installVersion+".apk");
		String apkFilePath = apkFile.getAbsolutePath();
		String reInstallFlag = (app.apiCore.installVersion == null) ? "" : " -r";
		if (forceReInstallFlag) reInstallFlag = " -r";
		Log.d(TAG, "Installing "+apkFilePath);
		try {
			boolean isInstalled = shellCommands.executeCommandAsRoot(
					"pm install"+reInstallFlag+" "+apkFilePath,
					"Success",context);
			apkFile.delete();
			return isInstalled;
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			apkFile.delete();
		} finally {
		}
		return false;
	}

}
