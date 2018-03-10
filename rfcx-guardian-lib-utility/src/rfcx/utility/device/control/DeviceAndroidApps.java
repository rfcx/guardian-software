package rfcx.utility.device.control;

import java.io.File;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import rfcx.utility.misc.ShellCommands;
import rfcx.utility.rfcx.RfcxLog;

public class DeviceAndroidApps {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceAndroidApps.class);
	
	public static final String[] cyanogenMod7AppsToDelete = new String[] { "Camera", "Calculator", "Browser", "Pacman", "Email", "FM", "Calendar", "Gallery", "Music", "QuickSearchBox", "VoiceDialer", "RomManager" };

	
	public static void deleteAndroidApps(String[] appsToDelete, Context context) {
		
		Log.d(logTag, "Deleting apps: "+TextUtils.join(", ", appsToDelete));
	
		ShellCommands.executeCommandAsRootAndIgnoreOutput("mount -o rw,remount /dev/block/mmcblk0p1 /system", context);
		
	  	StringBuilder executeAppDeletion = new StringBuilder();
	  	for (int i = 0; i < appsToDelete.length; i++) {
	  		executeAppDeletion.append("rm -f /system/app/").append(appsToDelete[i]).append(".apk; ");
	  	}
		
	  	ShellCommands.executeCommandAsRootAndIgnoreOutput(executeAppDeletion.toString(), context);
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
	
	
	public static void killAndReLaunchGuardianAppRoles(String[] appRoles, Context context) {
		
		StringBuilder shellCmd = new StringBuilder();
		
		for (String appRole : appRoles) {
			shellCmd.append("kill $(ps | grep org.rfcx.guardian.").append(appRole.toLowerCase()).append(" | awk '{print $2}')");
			shellCmd.append(" && ");
			shellCmd.append("am start -n org.rfcx.guardian.").append(appRole.toLowerCase()).append("/").append(appRole.toLowerCase()).append(".activity.MainActivity");
			shellCmd.append(";\n");
		}
				
		ShellCommands.executeCommandAsRootAndIgnoreOutput(shellCmd.toString(), context);
	}
	
//	public static void launchRfcxGuardianAppRole(String appRole/*, Context context*/) {
//		
//		ShellCommands.executeCommandAndIgnoreOutput(
//				(new StringBuilder())
//				.append("am start -n org.rfcx.guardian.").append(appRole)
//				.append("/").append(appRole).append(".activity.MainActivity").toString()
//				);
//	}
//	
//	public static void killRfcxGuardianAppRole(String appRole, Context context) {
//		
//		ShellCommands.executeCommandAsRootAndIgnoreOutput(
//			(new StringBuilder()).append("kill $(ps | grep org.rfcx.guardian.").append(appRole).append(" | awk '{print $2}')").toString(), context
//		);
//	}
	
	
}
