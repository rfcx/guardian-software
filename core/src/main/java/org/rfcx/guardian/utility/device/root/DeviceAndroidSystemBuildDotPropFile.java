package org.rfcx.guardian.utility.device.root;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;

public class DeviceAndroidSystemBuildDotPropFile {

	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceAndroidSystemBuildDotPropFile.class);

	private static final String origFilePath = "/system/build.prop";

	public static void updateBuildDotPropFile(String[] propertiesWithValues, Context context) {

		String tmpFilePath = context.getFilesDir().toString()+"/build.prop.tmp";
		String backupFilePathA = "/system/build.prop."+System.currentTimeMillis();
		String backupFilePathB = context.getFilesDir().toString()+"/build.prop."+System.currentTimeMillis();

		StringBuilder shellCmd = new StringBuilder();
		shellCmd.append("mount -o rw,remount /dev/block/mmcblk0p1 /system;\n");

		List<String> propertyKeys = new ArrayList<String>();
		for (String propertyWithValue : propertiesWithValues) {
			propertyKeys.add(propertyWithValue.substring(0, propertyWithValue.indexOf("=")));
		}
		shellCmd.append("cat ").append(origFilePath).append(" | grep -v -E \"").append(TextUtils.join("|", propertyKeys)).append("\" > ").append(tmpFilePath).append(";\n");;

		List<String> appendCmds = new ArrayList<String>();
		for (String propertyWithValue : propertiesWithValues) {
			appendCmds.add((new StringBuilder()).append("echo \"").append(propertyWithValue).append("\" >> ").append(tmpFilePath).toString());
		}
		shellCmd.append(TextUtils.join(" && ", appendCmds)).append(";\n");

		shellCmd.append("chmod 0644 ").append(tmpFilePath)
				.append(" && mv ").append(origFilePath).append(" ").append(backupFilePathA)
				.append(" && cp ").append(tmpFilePath).append(" ").append(origFilePath)
				.append(" && cp ").append(backupFilePathA).append(" ").append(backupFilePathB)
				.append(" && rm ").append(backupFilePathA).append(" ").append(tmpFilePath)
				.append(";\n");

		Log.i(logTag, "Updating System build.prop file for properties: "+TextUtils.join(" ", propertyKeys));

		Log.e(logTag, shellCmd.toString());
//		ShellCommands.executeCommandAsRootAndIgnoreOutput(shellCmd.toString(), context);

//		DeviceReboot.triggerForcedRebootAsRoot(context);
	}
	
}
