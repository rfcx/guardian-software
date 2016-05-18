package org.rfcx.guardian.utility.install;

import java.io.File;

import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.util.Log;

public class InstallUtils {

	private static final String logTag = "Rfcx-Utils-"+DbUtils.class.getSimpleName();
	
//	private static boolean installApk(Context context, String apkFileName, boolean forceReInstallFlag) {
//		
////		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
//		File apkFile = new File(context.getFilesDir().getPath(), apkFileName);
//		String apkFilePath = apkFile.getAbsolutePath();
//		String reInstallFlag = (app.apiCore.installVersion == null) ? "" : " -r";
//		if (forceReInstallFlag) reInstallFlag = " -r";
//		Log.d(TAG, "installing "+apkFilePath);
//		try {
//			boolean isInstalled = ShellCommands.executeCommand(
//					"pm install"+reInstallFlag+" "+apkFilePath,
//					"Success",true,context);
//			if (apkFile.exists()) { apkFile.delete(); }
//			return isInstalled;
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//			if (apkFile.exists()) { apkFile.delete(); }
//		} finally {
//		}
//		return false;
//		
//	}
	
}
