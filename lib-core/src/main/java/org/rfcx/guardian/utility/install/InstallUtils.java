package org.rfcx.guardian.utility.install;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

public class InstallUtils {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "InstallUtils");

	public InstallUtils(Context context, String appRole) {

		this.context = context;
		this.thisAppRole = appRole;

		this.apkDirDownload = context.getFilesDir().toString();
		this.apkDirExternal = Environment.getExternalStorageDirectory().toString()+"/rfcx/apk";;

		(new File(this.apkDirDownload)).mkdirs(); FileUtils.chmod(this.apkDirDownload,  "rw", "rw");
		(new File(this.apkDirExternal)).mkdirs(); FileUtils.chmod(this.apkDirExternal,  "rw", "rw");
	}

	private Context context;
	private String thisAppRole;
	public String apkDirDownload;
	public String apkDirExternal;
	public String apkFileName = null;
	public String apkFileNameDownload = null;
	public String apkPathDownload = null;
	public String apkPathPostDownload = null;
	public String apkPathExternal = null;


	public String installRole = null;
	public String installVersion = null;
	public String installReleasedAt = null;
	public String installVersionUrl = null;
	public String installVersionSha1 = null;
	public int installVersionValue = 0;

	public void setInstallConfig(String role, String version, String releasedAt, String url, String sha1, int versionValue) {
		this.installRole = role;
		this.installVersion = version;
		this.installReleasedAt = releasedAt;
		this.installVersionUrl = url + (url.contains("?") ? "&" : "?") + "released="+releasedAt;
		this.installVersionSha1 = sha1;
		this.installVersionValue = versionValue;


		this.apkFileName = this.installRole+"-"+this.installVersion+".apk";
		this.apkFileNameDownload = this.apkFileName+".gz";
		this.apkPathDownload = this.apkDirDownload+"/"+this.apkFileNameDownload;
		this.apkPathPostDownload = this.apkDirDownload+"/"+this.apkFileName;
		this.apkPathExternal = this.apkDirExternal+"/"+this.apkFileName;
	}

	public boolean installApkAndVerify() {

		try {
			Log.d(logTag, "Installing APK: "+apkPathExternal);

			// Before installation attempt, check version of target role, to determine if it even exists
			String targetRoleCurrentVersion = RfcxRole.getRoleVersionByName(installRole, thisAppRole, context);

			String[] installCommands = new String[] { "/system/bin/pm", "install", "-r", apkPathExternal };
			if (targetRoleCurrentVersion == null) installCommands = new String[] { "/system/bin/pm", "install", apkPathExternal };

			ShellCommands.executeCommand(TextUtils.join(" ", installCommands));

			// After installation attempt, check version of target role, to determine if it was successfully installed (and that it launches)
			targetRoleCurrentVersion = RfcxRole.getRoleVersionByName(installRole, thisAppRole, context);

			if (targetRoleCurrentVersion == null) {
				return false;
			} else {
				Log.i(logTag, "Current role version installed: "+installRole+", " + targetRoleCurrentVersion + " (attempted: " + installVersion+")");
				return (targetRoleCurrentVersion.equalsIgnoreCase(installVersion));
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return false;
	}


	public static int calculateVersionValue(String versionName) {
		try {
			int majorVersion = Integer.parseInt(versionName.substring(0, versionName.indexOf(".")));
			int subVersion = Integer.parseInt(versionName.substring(1+versionName.indexOf("."), versionName.lastIndexOf(".")));
			int updateVersion = Integer.parseInt(versionName.substring(1+versionName.lastIndexOf(".")));
			return 1000*majorVersion+100*subVersion+updateVersion;
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return 0;
	}


}
