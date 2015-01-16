package org.rfcx.guardian.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.rfcx.guardian.RfcxGuardian;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class ShellCommands {

	private static final String TAG = ShellCommands.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public void promptRootAuth() {
		try {
			Runtime.getRuntime().exec(new String[] {"su", "-c", "pm list features"});
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
	}
	
	public void grabScreenShot(Context context) {
		try {
			Process sh = Runtime.getRuntime().exec("su", null, null);
			OutputStream os = sh.getOutputStream();
			os.write(("/system/bin/screencap -p " + context.getFilesDir().getPath() + "/img.png").getBytes("ASCII"));
			Log.d(TAG,os.toString());
			os.flush();
			os.close();
			sh.waitFor();
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		} catch (InterruptedException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
	}
	
	public boolean installUpdatedApp(Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		File apkFile = new File(context.getFilesDir().getPath(), app.getPrefString("next_version")+".apk");
		String apkFilePath = apkFile.getAbsolutePath();
		String reInstallFlag = app.getPrefString("last_version").equals("0.0.0") ? "" : " -r";
		Log.d(TAG, "Installing "+apkFilePath);
		Process installApp = null;
		try {
			boolean isInstalled = false;
			installApp = Runtime.getRuntime().exec(new String[] {"su", "-c", "pm install"+reInstallFlag+" "+context.getFilesDir().getPath()+"/*.apk"});
			BufferedReader reader = new BufferedReader (new InputStreamReader(installApp.getInputStream()));
			String eachLine; while ((eachLine = reader.readLine()) != null) {
				if (eachLine.equals("Success")) {
					isInstalled = true;
				}
			}
			apkFile.delete();
			return isInstalled;
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
			apkFile.delete();
		} finally {
		}
		return false;
	}
	
	public void setSystemClock(long secondsSinceEpoch) {
		try {
			Runtime.getRuntime().exec(new String[] {"su", "-c", "date "+secondsSinceEpoch});
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
	}
	
	public void reboot(Context context) {
		try {
			Runtime.getRuntime().exec(new String[] {"su", "-c", "reboot"});
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
	}	
	
}
