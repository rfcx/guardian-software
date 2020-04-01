package org.rfcx.guardian.utility.device.root;

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

		String tmpFilePath = context.getFilesDir().toString()+"/build.prop.tmp";
		String backupFilePathA = "/system/build.prop."+System.currentTimeMillis();
		String backupFilePathB = context.getFilesDir().toString()+"/build.prop."+System.currentTimeMillis();

		List<String> propertyKeys = new ArrayList<String>();
		for (String propertyWithValue : propertiesWithValues) {
			propertyKeys.add(propertyWithValue.substring(0, propertyWithValue.indexOf("=")));
		}

//		StringBuilder shellCmd = new StringBuilder();

		List<String> appendCmds = new ArrayList<String>();
		for (String propertyWithValue : propertiesWithValues) {
			appendCmds.add((new StringBuilder()).append("echo \"").append(propertyWithValue).append("\" >> ").append(tmpFilePath).toString());
		}

		StringBuilder moveIntoPlace =
			(new StringBuilder()).append("chmod 0644 ").append(tmpFilePath)
				.append(" && mv ").append(origFilePath).append(" ").append(backupFilePathA)
				.append(" && cp ").append(tmpFilePath).append(" ").append(origFilePath)
				.append(" && cp ").append(backupFilePathA).append(" ").append(backupFilePathB)
				.append(" && rm ").append(backupFilePathA).append(" ").append(tmpFilePath);

		Log.i(logTag, "Updating System build.prop file for properties: "+TextUtils.join(" ", propertyKeys));

		ShellCommands.executeCommandAsRootAndIgnoreOutput("mount -o rw,remount /dev/block/mmcblk0p1 /system", context);
		ShellCommands.executeCommandAsRootAndIgnoreOutput((new StringBuilder()).append("cat ").append(origFilePath).append(" | grep -v -E \"").append(TextUtils.join("|", propertyKeys)).append("\" > ").append(tmpFilePath).toString(), context);
		ShellCommands.executeCommandAsRootAndIgnoreOutput(TextUtils.join(" && ", appendCmds), context);
		ShellCommands.executeCommandAsRootAndIgnoreOutput(moveIntoPlace.toString(), context);

//		DeviceReboot.triggerForcedRebootAsRoot(context);
	}
	
}
