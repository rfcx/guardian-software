package setup.service;

import java.io.File;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import setup.RfcxGuardian;

public class InstallAppService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, InstallAppService.class);
	
	private static final String SERVICE_NAME = "InstallApp";

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
			String apkFileName = app.apiCore.installRole+"-"+app.apiCore.installVersion+".apk";
			try {
				ShellCommands.killProcessByName(app.getApplicationContext(),"org.rfcx.guardian."+app.targetAppRole,"."+RfcxGuardian.APP_ROLE);
				successfullyInstalled = installApk(app.getApplicationContext(),apkFileName,false);
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				
				String apkFilePath = app.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+apkFileName;
				File apkFile = (new File(apkFilePath));
				
				if (successfullyInstalled) {
					Log.d(logTag, "installation successful ("+app.targetAppRole+", "+app.apiCore.installVersion+"). deleting apk and rebooting...");
					installLoopCounter = 0;
					if (apkFile.exists()) apkFile.delete();
					ShellCommands.executeCommand("reboot",null,false,app.getApplicationContext());
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
	
	private boolean installApk(Context context, String apkFileName, boolean forceReInstallFlag) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		File apkFile = new File(context.getFilesDir().getPath(), apkFileName);
		String apkFilePath = apkFile.getAbsolutePath();
		String reInstallFlag = (app.apiCore.installVersion == null) ? "" : " -r";
		if (forceReInstallFlag) reInstallFlag = " -r";
		Log.d(logTag, "installing "+apkFilePath);
		try {
			boolean isInstalled = ShellCommands.executeCommand(
					"pm install -f"+reInstallFlag+" "+apkFilePath,
					"Success",true,context);
			if (apkFile.exists()) { apkFile.delete(); }
			return isInstalled;
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
			if (apkFile.exists()) { apkFile.delete(); }
		} finally {
		}
		return false;
	}

}
