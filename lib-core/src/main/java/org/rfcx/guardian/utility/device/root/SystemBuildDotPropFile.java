package org.rfcx.guardian.utility.device.root;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SystemBuildDotPropFile {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "SystemBuildDotPropFile");

	private static final String origFilePath = "/system/build.prop";

	public static void updateBuildDotPropFile(String[] propertiesWithValues, Context context, boolean rebootWhenFinished) {

		String tmpFilePath = context.getFilesDir().toString()+"/build.prop.tmp";
		String backupFilePathA = "/system/build.prop."+System.currentTimeMillis();
		String backupFilePathB = context.getFilesDir().toString()+"/build.prop."+System.currentTimeMillis();

		String shellScriptFilePath = context.getFilesDir().toString()+"/update-build-dot-prop-x"+propertiesWithValues.length+".sh";
		File shellScriptFileObj = new File(shellScriptFilePath); if (shellScriptFileObj.exists()) { shellScriptFileObj.delete(); }

		List<String> propertyKeys = new ArrayList<String>();
		for (String propertyWithValue : propertiesWithValues) {
			propertyKeys.add(propertyWithValue.substring(0, propertyWithValue.indexOf("=")));
		}

		// Initialize shell script
		StringBuilder shellScriptContents = (new StringBuilder()).append("#!/system/bin/sh\n");

		// Cmd: Re-mount /system volume in r/w mode
		shellScriptContents.append("mount -o rw,remount /dev/block/mmcblk0p1 /system").append(";\n");

		// Cmd: Take a snapshot of the build.prop file, but exclude the lines containing the properties we intend to update/set
		shellScriptContents.append("cat ").append(origFilePath).append(" | grep -v -E \"").append(TextUtils.join("|", propertyKeys)).append("\" > ").append(tmpFilePath).append("\n");

		// Cmd: Iterate through each intended property+value and append them to the snapshot of build.prop
		List<String> appendCmds = new ArrayList<>();
		for (String propertyWithValue : propertiesWithValues) {
			appendCmds.add("echo \"" + propertyWithValue + "\" >> " + tmpFilePath);
		}
		shellScriptContents.append(TextUtils.join(" && ", appendCmds)).append(";\n");

		// Cmd: Set appropriate permissions and move/copy the new build.prop into the appropriate location
		shellScriptContents.append("chmod 0644 ").append(tmpFilePath)
				.append(" && mv ").append(origFilePath).append(" ").append(backupFilePathA)
				.append(" && cp ").append(tmpFilePath).append(" ").append(origFilePath)
				.append(" && cp ").append(backupFilePathA).append(" ").append(backupFilePathB)
				.append(" && rm ").append(backupFilePathA).append(" ").append(tmpFilePath)
				.append(";\n");

		shellScriptContents.append("echo 'Update of build.prop is complete. Reboot is required before changes will take effect.';\n");
		if (rebootWhenFinished) { shellScriptContents.append("echo 'Triggering Reboot now...';\n").append("reboot;\n"); }

		StringUtils.saveStringToFile(shellScriptContents.toString(), shellScriptFilePath);

		if ((new File(shellScriptFilePath)).exists()) {
			Log.i(logTag, "Update script created for build.prop, for properties: " + TextUtils.join(" ", propertyKeys));
			Log.i(logTag, "Script must be executed as root, likely ended with an immediate system reboot.");
			Log.i(logTag, "Script location: " + shellScriptFilePath);
		} else {
			Log.e(logTag, "Failed to save build.prop update shell script");
		}
	}
	
}
