package org.rfcx.guardian.installer.service;

import java.io.File;

import org.rfcx.guardian.installer.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class InstallAppService extends Service {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+InstallAppService.class.getSimpleName();

	private InstallApp installApp;

	private RfcxGuardian app = null;
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
		
		app = (RfcxGuardian) getApplication();
		if (context == null) context = app.getApplicationContext();
		
		if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
		
		app.isRunning_InstallApp = true;
		try {
			this.installApp.start();
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
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
			String apkFileName = app.apiCore.installRole+"-"+app.apiCore.installVersion+".apk";
			try {
				shellCommands.killProcessByName(context,"org.rfcx.guardian."+app.targetAppRole,"."+app.thisAppRole);
				successfullyInstalled = installApk(context,apkFileName,false);
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			} finally {
				
				String apkFilePath = context.getFilesDir().getAbsolutePath()+"/"+apkFileName;
				File apkFile = (new File(apkFilePath));
				
				if (successfullyInstalled) {
					Log.d(TAG, "installation successful ("+app.targetAppRole+", "+app.apiCore.installVersion+"). deleting apk and rebooting...");
					installLoopCounter = 0;
					if (apkFile.exists()) apkFile.delete();
					shellCommands.executeCommand("reboot",null,false,context);
				} else if ((installLoopCounter < 1) && (new FileUtils()).sha1Hash(apkFilePath).equals(app.apiCore.installVersionSha1)) {
					installLoopCounter++;
					app.triggerService("InstallApp", true);
				} else {
					Log.d(TAG, "installation failed ("+app.targetAppRole+", "+app.apiCore.installVersion+").  deleting apk...");
					installLoopCounter = 0;
					if (apkFile.exists()) apkFile.delete();
				}

				app.isRunning_DownloadFile = false;
				app.stopService("InstallApp");
			}
		}
	}
	
	private boolean installApk(Context context, String apkFileName, boolean forceReInstallFlag) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		ShellCommands shellCommands = new ShellCommands();
		File apkFile = new File(context.getFilesDir().getPath(), apkFileName);
		String apkFilePath = apkFile.getAbsolutePath();
		String reInstallFlag = (app.apiCore.installVersion == null) ? "" : " -r";
		if (forceReInstallFlag) reInstallFlag = " -r";
		Log.d(TAG, "installing "+apkFilePath);
		try {
			boolean isInstalled = shellCommands.executeCommand(
					"pm install"+reInstallFlag+" "+apkFilePath,
					"success",true,context);
			if (apkFile.exists()) { apkFile.delete(); }
			return isInstalled;
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			if (apkFile.exists()) { apkFile.delete(); }
		} finally {
		}
		return false;
	}

}
