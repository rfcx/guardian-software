package org.rfcx.guardian.updater.install;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;
import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.io.File;
import java.util.List;

public class InstallUtils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, InstallUtils.class);

	public InstallUtils(Context context) {

		this.apkDirDownload = context.getFilesDir().toString();

		(new File(this.apkDirDownload)).mkdirs(); FileUtils.chmod(this.apkDirDownload,  "rw", "rw");
		(new File(this.apkDirExternal)).mkdirs(); FileUtils.chmod(this.apkDirExternal,  "rw", "rw");
	}

	public String apkDirDownload = null;
	public String apkDirExternal = Environment.getExternalStorageDirectory().toString()+"/rfcx/apk";

	
}
