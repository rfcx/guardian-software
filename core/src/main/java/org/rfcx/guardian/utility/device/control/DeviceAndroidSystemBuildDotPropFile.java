package org.rfcx.guardian.utility.device.control;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;

public class DeviceAndroidSystemBuildDotPropFile {

	private static final String logTag = RfcxLog.generateLogTag("Utils", DeviceAndroidSystemBuildDotPropFile.class);

	private static final String origFilePath = "/system/build.prop";

	public static void updateBuildDotPropFile(String[] propertiesWithValues, Context context) {

		String tmpFilePath = context.getFilesDir().toString()+"/tmp.build.prop";
		String backupFilePath = context.getFilesDir().toString()+"/"+System.currentTimeMillis()+".build.prop";

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
				.append(" && mv ").append(origFilePath).append(" ").append(backupFilePath)
				.append(" && mv ").append(tmpFilePath).append(" ").append(origFilePath)
				.append(";\n");

		shellCmd.append("rm ").append(backupFilePath).append(";\n");

		Log.i(logTag, "Updating System build.prop file for properties: "+TextUtils.join(" ", propertyKeys));

		ShellCommands.executeCommandAsRootAndIgnoreOutput(shellCmd.toString(), context);

		DeviceReboot.triggerForcedRebootAsRoot(context);
	}
	
}
