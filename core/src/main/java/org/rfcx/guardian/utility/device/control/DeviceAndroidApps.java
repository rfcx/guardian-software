package org.rfcx.guardian.utility.device.control;

import java.io.File;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceAndroidApps {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceAndroidApps.class);
	
	public static final String[] cyanogenMod7AppsToDelete = new String[] { 
			"Camera", "Calculator", "Browser", "Pacman", "Email", "FM", "Calendar", "Gallery", "Music", "QuickSearchBox", "VoiceDialer", "RomManager" };

	
	public static void deleteAndroidApps(String[] appsToDelete, Context context) {
		
		Log.d(logTag, "Deleting apps: "+TextUtils.join(", ", appsToDelete));
	
		StringBuilder shellCmd = new StringBuilder();
		shellCmd.append("mount -o rw,remount /dev/block/mmcblk0p1 /system;\n");
		for (String appToDelete : appsToDelete) {
			shellCmd.append("rm -f /system/app/").append(appToDelete).append(".apk;\n");
		}
	  	ShellCommands.executeCommandAsRootAndIgnoreOutput(shellCmd.toString(), context);
	}
	

	public static boolean installAndroidApp(String apkFilePath, boolean isPreviouslyInstalled, Context context) {
		
		boolean wasSuccessful = false;
		
		File apkFileObj = new File(apkFilePath);
		Log.d(logTag, "Installing "+apkFilePath);
		try {
			if (apkFileObj.exists()) {
				wasSuccessful = ShellCommands.executeCommandAsRootAndSearchOutput((new StringBuilder()).append("pm install -f ").append( (isPreviouslyInstalled) ? "-r " : ""  ).append(apkFilePath).append(";").toString(), "Success", context);
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			if (apkFileObj.exists()) { apkFileObj.delete(); }
		}
		return wasSuccessful;
	}
	
	
	
	
	public static void killProcessesByName(String[] processNames, Context context) {
		StringBuilder shellCmd = new StringBuilder();
		for (String processName : processNames) {
			shellCmd.append("kill $(ps | grep ").append(processName.toLowerCase()).append(" | awk '{print $2}');\n");
		}
		ShellCommands.executeCommandAsRootAndIgnoreOutput(shellCmd.toString(), context);
	}
	
	public static void killProcessByName(String processName, Context context) {
		killProcessesByName(new String[] { processName }, context);
	}
	
	public static void launchGuardianAppRoles(String[] appRoles, Context context) {
		StringBuilder shellCmd = new StringBuilder();
		for (String appRole : appRoles) {
			shellCmd.append("am start -n org.rfcx.org.rfcx.guardian.guardian.").append(appRole.toLowerCase()).append("/").append(appRole.toLowerCase()).append(".activity.MainActivity;\n");
		}
		ShellCommands.executeCommandAsRootAndIgnoreOutput(shellCmd.toString(), context);
	}
	
	public static void launchGuardianAppRole(String appRole, Context context) {
		launchGuardianAppRoles(new String[] { appRole }, context);
	}
	
	public static void killAndReLaunchGuardianAppRoles(String[] appRoles, Context context) {
		StringBuilder shellCmd = new StringBuilder();
		for (String appRole : appRoles) {
			shellCmd.append("kill $(ps | grep org.rfcx.org.rfcx.guardian.guardian.").append(appRole.toLowerCase()).append(" | awk '{print $2}')");
			shellCmd.append(" && ");
			shellCmd.append("am start -n org.rfcx.org.rfcx.guardian.guardian.").append(appRole.toLowerCase()).append("/").append(appRole.toLowerCase()).append(".activity.MainActivity");
			shellCmd.append(";\n");
		}
		ShellCommands.executeCommandAsRootAndIgnoreOutput(shellCmd.toString(), context);
	}
	
	public static void killAndReLaunchGuardianAppRole(String appRole, Context context) {
		killAndReLaunchGuardianAppRoles(new String[] { appRole }, context);
	}
	
}
